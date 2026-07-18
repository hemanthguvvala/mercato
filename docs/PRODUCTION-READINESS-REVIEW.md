# Mercato — Production-Readiness Review & Forward Roadmap

> ⚠️ **SUPERSEDED (2026-07-18).** This 2026-07-09 review is kept for history but is now **stale in
> places** — several findings are fixed (reactor POM, shared `events`, RS256/JWKS auth, `/payments/**`
> behind a token, PENDING-first saga, refund/idempotency-key, per-event outbox, resilience4j) and it
> still describes the old HMAC-secret / `X-User` design the code has replaced.
> **Current, code-accurate backlog:** see [`PRODUCTION-FIX-PLAN.md`](./PRODUCTION-FIX-PLAN.md).
>
> **Date:** 2026-07-09 · **Reviewer:** senior-architect pass over all 8 services
> (`order`, `catalog`, `inventory`, `payment`, `notification`, `analytics`, `api-gateway`, `discovery`).
> **Method:** three deep audits — (1) config/scalability/maintainability, (2) security, (3) correctness/reliability.
> All findings are file:line-precise against the code as it stood on the review date.

## How to use this doc

This is the **answer key** for the interview we're running. If you want to keep the exercise honest,
**finish Round 1 + Round 2 of the interview first**, then read Parts 1–4. Each finding names the
**one concept to learn** — the fixes are yours to write (per our hands-on method), I only diagnose and teach.

**Severity legend:** 🔴 CRITICAL (money/data loss or auth bypass) · 🟠 HIGH (breaks under load/failure) ·
🟡 MEDIUM (real gap, not yet biting) · ⚪ LOW (polish / latent).

---

## The verdict (TL;DR)

**A genuinely strong *learning* build — not yet a *production* system.** You've correctly implemented the
*hard ideas* (saga, outbox, idempotency, optimistic vs pessimistic locking, resilience4j, cache-aside). The
gaps are the "unglamorous production" layer: an actual **auth boundary**, **crash-safe** distributed
transactions, **horizontal scalability**, and **tests on the risky core**.

**The five things that would fail a production readiness gate:**
1. **No authentication boundary anywhere except order-service** — catalog/inventory/payment/gateway are wide open, including the *money* endpoints (`/payments/charge`, `/payments/refund`).
2. **The saga wraps remote calls inside one `@Transactional`** → a commit failure after payment = *money taken, no order, no refund*.
3. **The outbox poller + consumers have ordering bugs** → duplicate storms, head-of-line blocking, and *lost events*.
4. **Cannot run more than one replica of anything** — H2 file DBs, in-memory analytics counters, and a poller that double-publishes.
5. **The riskiest code (saga, outbox, idempotency) has essentially zero tests.**

---

## Part 1 — 🔴 CRITICAL (fix before anything else)

**[R1] Saga runs remote calls *inside* a single `@Transactional`** — `order-service` `service/OrderService.java:51-89`
`create()` is `@Transactional` and calls `getProduct` (55), `reserve` (66), `charge` (69), then commits the order at method end. If the commit fails *after* the card is charged, `@Transactional` rolls back the order row but **cannot** un-charge or un-reserve → money taken, stock gone, no order, no refund. Also pins a DB connection across 3 network hops (pool exhaustion).
*Concept:* persist the order **PENDING and commit before** any remote call; drive compensation off saved state; keep transactions short and I/O-free.

**[R2] Compensation misses timeouts and never refunds** — `order-service` `service/OrderService.java:70-77`
The catch is `FeignException | WebClientResponseException`. A payment **timeout** throws `WebClientRequestException` (not caught) → inventory never released, charge never refunded. And `PaymentClient.refund` exists but is **never called** on any path.
*Concept:* compensate on "the side-effect *might* have happened" (timeouts included); every forward action needs its inverse (reserve↔release, charge↔refund).

**[R3] No authentication boundary except order-service** — `api-gateway/ApiGatewayApplication.java` (no security), `catalog/inventory/payment` (no `spring-boot-starter-security`, no `SecurityFilterChain`)
The gateway is a bare reverse proxy — no JWT validation, no token relay. `catalog` (POST/PUT/DELETE products), `inventory` (reserve/release, incl. gRPC :9090), and **`payment` (charge/refund)** accept unauthenticated requests from anyone who can reach the port. Order-service's clients also send no credentials.
*Concept:* authenticate once at the gateway (OAuth2/JWT resource server), then propagate a **verified** identity to zero-trust downstream services (mTLS or a service token).

**[R4] JWT signing secret committed to the repo** — `order-service/application.properties:32` → `JwtService.java:22`
`jwt.secret-key=local-dev-only-change-me...` is the HMAC key that actually runs (nothing enforces the env override). Anyone with repo access can forge a token for `admin`.
*Concept:* signing keys come from a secret store/env at runtime, never source; rotating a secret must invalidate old tokens. **Also rotate the leaked Upstash Redis token** (long-standing TODO).

**[R5] Outbox: per-batch rollback = duplicate storm + poison = head-of-line block** — `order-service` `service/OutboxPoller.java:31-46`
`publishPending()` is one `@Transactional` looping over `findByPublishedFalse()`; `published=true` flags flush only at batch commit. If event #3 fails to send, the whole tx rolls back and **already-sent #1/#2 are re-sent** next poll. A poison payload re-sends forever and blocks all events behind it.
*Concept:* mark-and-commit **per event**; quarantine poison rows (retry counter → dead-letter); pair with an idempotent producer.

**[R6] Consumer idempotency marks the key *before* processing → lost events** — `notification-service/NotificationListener.java:22-30`, `analytics-service/AnalyticsListener.java:25-33`
Both call `setIfAbsent(key)` **first**, then do the work. If the work throws / the pod crashes before the offset commits, redelivery sees the key present → **skips** → event permanently lost. This turns at-least-once *delivery* into at-most-once *processing*.
*Concept:* "process, **then** mark" (or make mark+side-effect atomic). The dedup key must mean *completed*, not *started*.

---

## Part 2 — 🟠 HIGH

**[R7] Outbox poller runs on every replica, no leader/lock, no message key** — `OutboxPoller.java:33-38`, `OutboxRepository.java:13`
Every `order-service` instance runs the `@Scheduled` job; both grab the same rows → double-publish. Sends with **no key** → no per-order partition ordering. `findByPublishedFalse()` is unbounded (loads full backlog into memory).
*Concept:* single-writer pollers (`SELECT … FOR UPDATE SKIP LOCKED` / ShedLock / leader election); partition-key selection; batch limits.

**[R8] No `ErrorHandlingDeserializer` / DLQ on consumers → poison pill stuck partition** — `notification/application.properties:16-24`, `analytics/application.properties:12-18`
A malformed message fails deserialization *before* the listener runs → offset can't advance → infinite redelivery blocks the partition. Listener-level failures drop the record after 10 tries with no DLQ (silent loss).
*Concept:* `ErrorHandlingDeserializer` + `DeadLetterPublishingRecoverer`.

**[R9] Live reserve path is optimistic-with-no-retry → spurious 500s** — `inventory-service` `InventoryController.java:24` → `InventoryService.reserve:20`, `exception/GlobalExceptionHandler.java:20`
REST + gRPC call the **optimistic** `reserve()`. Under contention the loser throws `ObjectOptimisticLockingFailureException`, unmapped → **HTTP 500** → order-service sees a 5xx and fails a legitimate order. The clean `reservePessimistic()` (`FOR UPDATE`) is proven by the test but **never wired to an endpoint**.
*Concept:* retry-on-optimistic-failure (or wire the pessimistic path); map lock conflicts → 409, not 500.

**[R10] Compensation loop can partially fail and mask the real error** — `OrderService.java:72-76`
If any `release(r)` throws mid-loop, the remaining items are never released (**stock leak**), `OrderFailedException` is never thrown, and a raw `FeignException` propagates → generic 500 instead of 409.
*Concept:* compensations must be best-effort **per-item** try/catch, idempotent, retried independently.

**[R11] No idempotency key on `POST /orders`** — `OrderController.java:30`, `OrderService.java:52`
A client timeout + retry creates a *second* order, reserves again, charges again. Nothing correlates the attempts.
*Concept:* client-supplied `Idempotency-Key` persisted uniquely to collapse retries.

**[R12] Resilience4j: 404s get retried; load-shed rejections trip the breaker** — `client/catalog/CatalogGateway.java:23-36`, `application.properties:37-56`
`@Retry` is outermost; the fallback on the inner `@CircuitBreaker` translates `FeignException.NotFound` → `ResourceNotFoundException` *before* Retry's `ignore-exceptions` (which lists the Feign type) sees it → **404s retried 3×**. `bulkhead.max-wait=0` / `ratelimiter.timeout=0` throw `BulkheadFull`/`RequestNotPermitted`, which are retried *and* counted as breaker failures → breaker opens from self-inflicted load-shedding.
*Concept:* aspect ordering + fallback placement; keep load-shed exceptions out of both the retry set and the breaker's failure set.

**[R13] H2 web console shipped + `permitAll`** — `order/catalog/inventory application.properties:10-11`, `SecurityConfig.java:31`
A full SQL shell (H2 has RCE history via `CREATE ALIAS`/`INIT`), unprotected on catalog/inventory.
*Concept:* dev-only tooling off in the prod profile.

**[R14] Authorization never enforced — roles defined, never checked** — `SecurityConfig.java:23,33,48-52`
`@EnableMethodSecurity` is on but there is **no** `@PreAuthorize`/`hasRole` anywhere. `/audit` returns the full audit log to any authenticated user; `admin` grants nothing.
*Concept:* method/URL-level authorization + least privilege (OWASP A01 broken access control).

**[R15] Hardcoded app credentials in source** — `SecurityConfig.java:48-52` (`hemanth/password123`, `admin/admin123`)
BCrypt-hashed at rest (good) but plaintext committed and unrotatable without redeploy; `admin123` is trivially guessable.
*Concept:* externalized user store (DB/IdP), never credentials in code.

**[R16] Cannot horizontally scale — H2 file DBs + in-memory analytics** — datasources `jdbc:h2:file:./data/{order,catalog,inventory}db`; `analytics-service/AnalyticsListener.java:14-15,31-33`
File-mode H2 lives in one process's filesystem (unshareable, non-durable across restarts). Analytics `totalRevenue`/`orderCount` are instance fields → wrong the moment you run >1 pod (each counts only its partitions) and reset on restart.
*Concept:* shared external Postgres as the precondition for stateless services; externalize aggregation state (Redis/DB/stream store).

**[R17] Critical paths essentially untested** — `order-service` has **no `src/test`** at all; payment/notification/analytics/gateway none; catalog only `contextLoads` + GraphQL slice; inventory has one good concurrency test (on in-mem H2, not the file DB it ships).
The saga, compensation, and outbox — the riskiest logic — have **zero coverage**. No Testcontainers, no integration test spanning Kafka/outbox/saga.
*Concept:* Testcontainers integration tests for the saga + outbox + Kafka round-trip.

**[R18] No dev/prod profiles; infra endpoints baked into the jar** — every `application.properties` (there are no YAML/profile files)
Eureka URL, Kafka bootstrap, datasource URL are all literals. You cannot promote one artifact dev→prod without editing source and rebuilding.
*Concept:* Spring profiles + externalized config ("one artifact, many environments").

---

## Part 3 — 🟡 MEDIUM

**[R19] Kafka: 1 partition + no consumer concurrency = no parallelism** — no `NewTopic` bean anywhere; `order-events` auto-created with 1 partition; no `spring.kafka.listener.concurrency`. Only one consumer per group is ever active. *Concept:* partitions as the unit of consumer parallelism.

**[R20] No connection-pool sizing, no client timeouts** — no Hikari config (defaults to 10); no Feign/`WebClient` connect/read timeouts (`WebClientConfig.java`). A slow downstream hangs request threads indefinitely. `open-in-view` only set false in order-service. *Concept:* pool sizing + timeouts as a resilience primitive; pair a `TimeLimiter` with the breaker.

**[R21] Kafka producer `acks`/idempotence not set** — `order/application.properties:58-59` sets only serializers. For an outbox you want `acks=all` + `enable.idempotence=true` + bounded retries. *Concept:* producer durability semantics.

**[R22] Actuator unauthenticated + `health.show-details=always`** — `SecurityConfig.java:32`, exposure lists in every service. Leaks datasource URLs, Redis host, disk paths, versions to anonymous callers (no auth at all on catalog/inventory/payment). *Concept:* secure/limit Actuator; gate health details behind a role. (Sensitive endpoints `env`/`heapdump`/`shutdown` are correctly *not* exposed.)

**[R23] Gateway rate-limit key is a spoofable client header** — `ApiGatewayApplication.java:19` reads `X-User` straight from the request; not stripped. Rotate the header → bypass the limiter; a foot-gun if any service later trusts `X-User` as identity. *Concept:* derive keys from a validated token claim; strip client-supplied identity headers at the edge.

**[R24] Kafka plaintext, no auth; `trusted.packages=*`** — `bootstrap-servers=localhost:9092` no TLS/SASL; `notification:24`/`analytics:18`. Mitigated by `use.type.headers=false` + fixed `value.default.type`, but the wildcard is a latent deserialization risk. *Concept:* Kafka TLS+SASL/ACLs; pin trusted packages.

**[R25] Stale framework, no dependency scanning** — all poms on Spring Boot **3.3.5** (Oct 2024), stale by mid-2026; illustrative CVEs affect the pinned Spring Security/Tomcat. *Concept:* build-time vuln scanning (OWASP Dependency-Check / Dependabot) + patch cadence.

**[R26] Redis host hardcoded in gateway** — `api-gateway/application.properties:16` = real Upstash hostname (every other service uses `${REDIS_HOST}`). *Concept:* 12-factor — externalize backing-service coordinates everywhere.

**[R27] `show-sql`/`format_sql` on + 100% trace sampling** — order/catalog/inventory + five services at `sampling.probability=1.0`. Huge log/CPU/exporter overhead + data leak in prod. *Concept:* env-specific observability tuning.

**[R28] Error-handling inconsistencies** — generic `Exception` handler in `order-service/web/GlobalExceptionHandler.java:36-39` **logs nothing** (500s vanish); `payment .../GlobalExceptionHandler.java:22` puts `ex.getMessage()` into the client body (leak); genuine problems logged at `log.info`. *Concept:* log full detail server-side at ERROR, expose a safe generic message.

**[R29] Saga has no durable state — crash mid-flight orphans reservations** — `OrderService.create():63-77`. Compensation lives only in an in-memory try/catch; a JVM death after reserve leaves stock reserved with no record to recover from. *Concept:* persistent saga state + idempotent downstream ops. (Root cause shared with R1.)

**[R30] No parent/reactor POM or shared BOM** — 8 poms each repeat the Boot parent + Spring Cloud import + versions. Versions align today but nothing enforces it. *Concept:* Maven multi-module parent + BOM.

**[R31] Contract/DTO classes copy-pasted across services** — `OrderPlaced` duplicated in 3 packages; `StockRequest`/`ChargeRequest` duplicated. A producer field rename silently breaks consumers at runtime. *Concept:* a shared `events`/contracts module (or schema registry) as single source of truth.

**[R32] `System.out.println` used as logging** — `PaymentService.java:19,23`, both listeners. Bypasses log levels, trace/correlation IDs, and aggregation. No `logback-spring.xml` anywhere. *Concept:* SLF4J parameterized logging + per-env config.

**[R33] Consumers have no health endpoint; no graceful shutdown** — notification/analytics use plain `spring-boot-starter` (no actuator) → K8s can't probe them; a wedged consumer looks healthy forever. No `server.shutdown=graceful`, no liveness/readiness split, Prometheus registry only in order-service. *Concept:* Actuator health probes (liveness vs readiness) + graceful shutdown for zero-downtime rollouts.

---

## Part 4 — ⚪ LOW

- **[R34]** Demo cruft in prod code: `/orders/{id}/itemcount-bad` (N+1) + `HelloController`; root package `com.interview.*` on a "Mercato" project; typos leak into APIs (`InSufficientStockException`, "No invetory for prodcut"). — *hygiene / removing scaffolding is part of "done".*
- **[R35]** Magic numbers/strings: topic `"order-events"` in 3 places, TTLs, `fixedDelay=2000`, credentials. — *promote to constants / `@ConfigurationProperties`.*
- **[R36]** JWT validates only signature+expiry (no iss/aud, platform-default charset on the key). — *JWT claim best-practice.*
- **[R37]** GraphQL: introspection on, no depth/complexity cap, no auth. — *harden GraphQL for prod.*
- **[R38]** `LoggingAspect` logs full args/return at INFO → PII (customer names/order contents) in logs. — *scrub PII from logs.*
- **[R39]** reserve/release not idempotent (no reservation id) → a retried `release` over-credits stock. — *idempotent inventory ops.*
- **[R40]** gRPC vs REST return different shapes for the same failure (`success=false` vs 409). — *consistent contracts.*
- **[R41]** CSRF disabled globally + no explicit CORS policy (fine for bearer-token API today; a trap when the React client is added). — *when CSRF applies + explicit CORS.*

---

## Cross-reference — your five questions → findings

| Concern | Findings |
|---|---|
| **Configuration** | R18, R26, R27, R20, R22 |
| **Scalability** | R16, R7, R19, R1 (connection pinning), R29, R20, R33 |
| **Security** | R3, R4, R13, R14, R15, R22, R23, R24, R25, R36, R37, R41 |
| **Error handling** | R2, R5, R6, R8, R10, R12, R28, R33 |
| **Maintainability** | R17, R30, R31, R32, R34, R35, R38 |

---

## What's genuinely done well (keep — and say these in interviews)

- **Transactional outbox capture is correct** — event saved in the *same* tx as the order (the hard part). Poller waits for the broker ack before marking published.
- **Idempotent consumers** via Redis `setIfAbsent`+TTL — right tool (ordering is the only flaw).
- **Both oversell defenses are real and proven** — `@Version` optimistic + `FOR UPDATE` pessimistic, with a genuine 20-thread stampede test asserting no negative stock.
- **Resilience4j** stack (CB+retry+bulkhead+rate-limiter with a real fallback) — well beyond typical portfolio depth; correctly ignores 404 on the breaker.
- **Flyway + `ddl-auto=validate`** everywhere a DB exists — no runtime schema drift (beginners get this wrong; you didn't).
- **Passwords BCrypt-hashed; stateless short-lived JWT; no SQL injection** (all JPA/bound params); **Bean Validation** on DTOs; sensitive actuator endpoints correctly not exposed.
- **Consistent build baseline** (Boot 3.3.5 / Java 17 / Spring Cloud 2023.0.3 across all 8); `open-in-view=false` + `join fetch` show N+1 awareness; audit log uses `REQUIRES_NEW`.

---

## Prioritized fix order (highest leverage first)

1. **R4 + rotate the Upstash token** — cheap, urgent, stops token forgery/leak.
2. **R3** — authenticate at the gateway, propagate verified identity downstream. This one design idea closes the two auth CRITICALs and makes the money endpoints unreachable from outside.
3. **R1 + R2 + R29** — refactor the saga: persist PENDING before remote calls, move I/O out of the tx, compensate on timeouts, add the refund. (This is the crown-jewel interview story.)
4. **R5 + R6 + R7 + R8** — make the outbox/consumers crash-safe: per-event commit, process-then-mark, SKIP LOCKED, DLQ.
5. **R16** — move to shared Postgres + externalize analytics state (unblocks *all* scaling).
6. **R17** — Testcontainers integration tests for saga + outbox (so you can refactor safely).
7. **R18 + R26 + R27** — profiles + externalized config; then R13/R14/R15/R22 (security hardening).
8. Then the MEDIUM/LOW polish as you touch each area.

---

## Forward roadmap (reconciled with `docs/ROADMAP.md`)

Run **two tracks in parallel** from here:

### Track A — Production Hardening (NEW — the fixes above)
Sequence = the prioritized fix order. This is where the *real* senior learning is. Each fix maps to a concept
you'll implement yourself. Target: a system that would survive `2 replicas + a chaos test`.

### Track B — Finish the feature phases
- **Phase 5 (finish):** gRPC client + saga wiring; GraphQL N+1 lesson. *(WebClient + GraphQL done.)*
- **Phase 6 (finish):** run the live Zipkin trace end-to-end; add inventory sampling; Grafana dashboard.
- **Phase 7:** Dockerfile per service (multi-stage) → docker-compose → Kubernetes (Deployments/Services/ConfigMaps/Secrets/Ingress/HPA) → Helm. **This is also where several scale fixes land** (ClusterIP enforces the gateway boundary; Secrets solve R4; K8s DNS can retire Eureka).
- **Phase 8 — Frontend: ⚠️ REACT (changed from Angular).** Storefront (browse catalog, place order) + admin. Talks to the gateway (REST + GraphQL), JWT login/refresh. *(This changes the learning track — React hooks/router/state + a data-fetching lib, vs Angular. Roadmap + README to be updated.)*
- **Phase 9:** portfolio polish — top-level README (architecture money-shot + honesty note), per-service READMEs, CI/CD (GitHub Actions), seed/demo data, decisions doc.

### Features worth adding (beyond fixes)
- Order **status endpoint + state machine** (PENDING/CONFIRMED/FAILED) — falls out of the R1 fix.
- **Dead-letter topic + a small "failed events" admin view** (pairs with R8).
- **Auth service / login + refresh** feeding the React app (pairs with R3).
- **Product search/pagination** in catalog (real e-commerce need; exercises GraphQL well).

---

*Nothing in this repo was modified to produce this review. Fixes are yours to implement — I diagnose, you code, we review.*
