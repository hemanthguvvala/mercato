# Mercato — Production Fix Plan (master backlog)

> **Date:** 2026-07-18 · **Method:** 7-way parallel deep audit of the *current* code
> (architecture/sync-comms · Kafka/EDA · concurrency/saga · resilience/error-handling ·
> scalability · observability · security). Every finding was verified against `src/main`
> (not the docs) with file:line evidence.
>
> **How to use this:** the findings are in **fix order** (`F1` highest leverage first) so you
> can work top-to-bottom, one by one. Each has a checkbox. Fixes are **yours to write** —
> this doc diagnoses, names the concept, and gives the algorithm; you implement, I review.
>
> **Severity:** 🔴 CRITICAL (money/data loss, auth bypass, or blocks all scale) ·
> 🟠 HIGH (breaks under load/failure/scale-out) · 🟡 MEDIUM (real gap) · ⚪ LOW (polish/latent).
>
> **Supersedes** `PRODUCTION-READINESS-REVIEW.md` (2026-07-09), which is now stale in places
> (still describes the old HMAC-secret / `X-User` design the code has since replaced).

---

## Verdict (why this list exists)

**Production-*shaped*, not production-*ready*.** The distributed-systems *patterns* are genuinely
senior-level and mostly correct. The blocker is the *substrate*: as wired, **no request-serving
service can run more than one replica**, so "millions of requests" is impossible until that changes.

- **Millions of requests?** No — capped at ~10 req/s (browse) and structurally at 1 replica each.
- **Scale hot services independently?** Not today — needs the datastore + per-instance-state fixes below.
- **Error handling on failure?** Good for a single request in isolation; unsafe under crash/load/outage.
- **Traceability & observability?** Weak — trace dies at the Kafka boundary; metrics on 1 service.
- **Kafka / multithreading / sync / EDA — real or nominal?** Multithreading + sync: real. Kafka:
  real tool but one pipeline. EDA as an *architecture*: nominal (core flow is synchronous orchestration).

## ✅ Done so far on this plan (updated 2026-07-19)

Working top-to-bottom, hands-on (you code, I review). Closed so far:

- **[x] F1** — H2 console gated behind `${H2_CONSOLE:false}`, off by default (order/catalog/inventory).
- **[x] F2** — auth seed creds externalized to `${SEED_USER_PASSWORD}`/`${SEED_ADMIN_PASSWORD}`; no usable default.
- **[x] F3** — all 5 stateful services (order, inventory, catalog, auth, review) migrated H2-file → **Neon Postgres**, database-per-service. Root cause of the "Unsupported Database: PostgreSQL" wall: the `flyway-database-postgresql` module was missing (flyway-core alone can't speak Postgres). Flyway kept.
- **[~] F31 (part)** — **Spring Boot 3.3.5 → 3.5.15** and **Spring Cloud 2023.0.3 → 2025.0.3 (Northfields)**; 14/14 modules BUILD SUCCESS. Still open: Dependabot/OWASP/Trivy CVE-scan gate.
- **[x] Slice 0 + Slice 1** (see `TARGET-ARCHITECTURE.md` §6) — substrate done; **order state machine** built (`OrderStatus` guard + `OrderEntity.transitionTo()` + append-only `OrderStatusHistory`), 9 unit tests green.
- **[x] Slice 2** — durable saga: `OrderReconciler` (`@Scheduled`) recovers stuck orders forward/backward off order status + an `updatedAt` heartbeat; `confirm()` guarded inside `create()`; `server.shutdown=graceful`; **★ inventory reservation-expiry sweep** (absorbs **F4, F5, F12, F13**). 8 unit tests green.
- **[x] Slice 3** — **F9 closed**: payment is now a durable Postgres ledger (`Payment` row per order; `unique(order_id)` is the exactly-once guard); in-memory maps deleted. 5 unit tests green.

**Next:** Slice 4 — real events + outbox/inbox (EDA becomes real; also closes the reservation-`CONFIRMED` seam from Slice 2).

---

## Already fixed since 2026-07-09 — do NOT redo (and say these in interviews)

Reactor/parent POM · shared `events` contract module · real RS256/JWKS auth with `/payments/**`
now behind a token · catalog writes + audit ADMIN-gated · PENDING-first saga (no DB connection
pinned across network hops, `open-in-view=false`) · refund + timeout-aware compensation ·
DB-backed `Idempotency-Key` · pessimistic `FOR UPDATE` + reservation-record idempotency on the real
reserve path · `acks=all` + idempotent producer · per-event outbox commit + message key · Kafka DLQ
(`ErrorHandlingDeserializer` + `DeadLetterPublishingRecoverer`) · **resilience4j configured
textbook-correct** (the two classic bugs — 404-retried, breaker-trips-on-load-shed — are NOT present).

---

## The fix checklist (work top-to-bottom)

### Phase 0 — Stop the bleeding (cheap, urgent)
- [x] **F1** 🔴 Disable the H2 web console by default + gate it ✅
- [x] **F2** 🔴 Remove weak default admin credentials from committed config ✅

### Phase 1 — The scale unlock
- [x] **F3** 🔴 Migrate the 5 stateful services from H2-file to shared Postgres ✅

### Phase 2 — Make the saga crash-safe
- [ ] **F4** 🔴 Durable saga state + reconciler; move `confirm()` inside the try
- [ ] **F5** 🟠 Graceful shutdown on all request-serving services (esp. order)
- [ ] **F12** 🟠 Catch `RejectedExecutionException` from the saga executor
- [ ] **F13** 🟠 Durable, retried compensation (stop swallowing release/refund failures)
- [ ] **F14** 🟠 Don't poison the idempotency key on a failed order

### Phase 3 — Resilience on the money path
- [ ] **F6** 🟠 Circuit breaker + TimeLimiter on payment & inventory (kill the cascade)
- [ ] **F18** 🟡 Gateway: downstream response timeout + circuit breaker
- [ ] **F19** 🟡 Gateway rate-limiter: decide fail-open vs closed; make INCR/EXPIRE atomic

### Phase 4 — Fix the per-instance state that blocks HA
- [ ] **F8** 🟠 Auth: one shared/persisted signing key (not regenerated per boot)
- [x] **F9** 🟠 Payment idempotency → Redis/DB with TTL (fixes double-charge + OOM) ✅
- [ ] **F10** 🟠 Outbox: leader lock / `SKIP LOCKED`, bound the fetch, index `published`

### Phase 5 — Authorization
- [ ] **F7** 🟠 Role checks on `/payments/**` + `/inventory/**`; object-level authz on `GET /orders`

### Phase 6 — Edge throughput
- [ ] **F11** 🟠 Rate-limit: fix the `"anonymous"` key, per-route quotas, cover `/auth` + `/orders`

### Phase 7 — Observability
- [ ] **F15** 🟠 Tracing across Kafka + all 10 services + configure a real collector
- [ ] **F16** 🟠 Prometheus on all services + custom business metrics
- [ ] **F24** 🟡 `logback-spring.xml`: structured/JSON logs, rolling, per-env; profile the sampling
- [ ] **F25** 🟡 Remove PII from logs
- [ ] **F29** 🟡 Propagate trace/MDC to the saga + audit executor threads
- [ ] **F30** 🟡 Secure `/actuator/prometheus` + Swagger; liveness/readiness on all services

### Phase 8 — Data hygiene
- [ ] **F17** 🟠 Pagination on all default list endpoints; bound audit + outbox queries
- [ ] **F32** ⚪ Caching on catalog list/search + a review-rating aggregate

### Phase 9 — Kafka hardening
- [ ] **F21** 🟡 Analytics dedup: claim-before-effect, atomic
- [ ] **F22** 🟡 Topic `replicas > 1`, schema registry, a DLT drain/reprocess consumer
- [ ] **F23** 🟡 Kafka TLS/SASL + ACLs
- [ ] **F28** 🟡 Inventory contention → 409 + retry-on-reserve
- [ ] **F38** ⚪ Emit `OrderFailed` / `OrderCancelled` events (only happy-path event exists)

### Phase 10 — Config, deps, hygiene
- [ ] **F20** 🟡 Secure or delete the unauthenticated gRPC surface; use the locked+idempotent reserve
- [ ] **F26** 🟡 Dev/prod Spring profiles; safe-by-default fallbacks
- [ ] **F27** 🟡 `@RestControllerAdvice` on catalog/review/auth; fix `IllegalState→404`; stop leaking cause
- [~] **F31** 🟡 Dependency/CVE scanning (OWASP/Dependabot/Trivy) + Spring Boot upgrade — ✅ Boot/Cloud upgraded; ⏳ CVE scan pending
- [ ] **F33** ⚪ Delete dead code (Feign `PaymentClient`, `reservePessimistic()`, unused gRPC)
- [ ] **F34** ⚪ Plan for the single Upstash Redis SPOF
- [ ] **F35** ⚪ Eureka eviction tuning + retry on the remaining sync edges
- [ ] **F36** ⚪ `@Valid` on catalog `PUT update()`
- [ ] **F37** ⚪ Strip client-supplied `X-User`/identity headers at the gateway
- [ ] **F39** ⚪ Fix stale comments/docs (`JwtService` says HS256; code is RS256)

---

## Detailed findings

### F1 🔴 Unauthenticated H2 console = arbitrary SQL
**Where:** `order-service` `SecurityConfig.java:21` + `application.properties:10`; `catalog` `SecurityConfig.java:20` + `application.properties:14`; `inventory` `SecurityConfig.java:18` + `application.properties:14` (`spring.h2.console.enabled=${H2_CONSOLE:true}`, `permitAll`, `sa`/blank).
**Impact:** anyone who can reach the port gets a full SQL shell — reads/writes every table, bypassing all auth above it. H2 also has RCE history via `CREATE ALIAS`. This is the real unauthenticated breach path.
**Fix (concept — dev-only tooling off in prod):** default `H2_CONSOLE` to `false`; only enable under a `dev` profile; never `permitAll` the console in a profile that ships. Falls out naturally once F26 (profiles) lands.
**✅ Done (2026-07-19):** `spring.h2.console.enabled=${H2_CONSOLE:false}` across order/catalog/inventory — off unless explicitly turned on.

### F2 🔴 Weak default admin credentials in committed config
**Where:** `auth-service/application.properties:30-31` — `admin/admin123`, `hemanth/password123` as `${...:default}` (the default is used whenever the env var is unset).
**Impact:** `java -jar` with no env → instant ADMIN, which defeats every role gate (F7) and the catalog/audit protections.
**Fix (concept — no credentials in source):** no usable default; fail fast if the secret/user store isn't configured. Move the user store to the DB/IdP (see the auth Track B in the enterprise roadmap) so accounts aren't compile-time constants.
**✅ Done (2026-07-19):** seed passwords are now `${SEED_USER_PASSWORD}`/`${SEED_ADMIN_PASSWORD}` with no default — the app won't hand out a known admin on a bare `java -jar`. (User store → DB/IdP is still the longer-term move.)

### F3 🔴 H2-file datastore → cannot run >1 replica of any stateful service
**Where:** `catalog application.properties:8`, `order:23`, `inventory:8`, `auth:20`, `review:4` — all `jdbc:h2:file:./data/...`. Postgres driver is on some classpaths and URLs are `${DB_URL:...}`-overridable, but **nothing sets `DB_URL`**, and `order-service/docker-compose.yml` starts a Postgres container no app connects to.
**Impact:** H2 file is single-process + file-locked. Two replicas either collide on the lock or diverge into private DBs. **This is the master scale blocker** — every request-serving service is pinned to exactly one instance.
**Fix (concept — shared external DB as the precondition for stateless services):** per-service Postgres DB/schema; set `DB_URL`/creds via env; keep Flyway; add `flyway-database-postgresql` where missing (inventory has no PG driver yet). Verify with 2 replicas pointing at the same DB. Then re-check pool math: Hikari `max=10 × N replicas` must stay under Postgres `max_connections` (~100) → consider PgBouncer.
**✅ Done (2026-07-19):** all 5 stateful services on **Neon Postgres** (database-per-service), creds via `${DB_USERNAME}`/`${DB_PASSWORD}`, Flyway migrations run clean. The "Unsupported Database: PostgreSQL" wall was **not** a version issue — it was the missing `flyway-database-postgresql` module (flyway-core can't speak PG alone); now added everywhere (catalog's PG deps also promoted out of `<scope>test</scope>`). ⏳ Still to validate: pool math / PgBouncer once we actually run `max=10 × N` replicas.

### F4 🔴 Non-durable saga compensation + no reconciler; `confirm()` outside the try
**Where:** `order-service/service/OrderService.java:74-96` (in-memory `try/catch`, no durable saga log, no reconciler; `confirm()` at `:96` is *outside* the catch). Only `@Scheduled` job is the outbox poller.
**Impact:** JVM death (or SIGTERM — see F5) after the charge, or a `confirm()`/DB failure after the charge, leaves **card captured + stock decremented + order stuck PENDING + no event + nothing to recover it.** This defeats the single most important saga property.
**Fix (concept — durable saga log + reconciliation + idempotent steps):** persist saga/attempt state (or an order status machine PENDING→CHARGED→CONFIRMED/COMPENSATING/FAILED) and write a compensation *intent* before each remote call; move `confirm()` inside the protected region; add a `@Scheduled` reconciler that scans stale PENDING/COMPENSATING orders and drives release+refund idempotently. Downstream ops are already idempotent — lean on that.

### F5 🟠 No graceful shutdown on request-serving services
**Where:** `server.shutdown=graceful` is set **only** on notification + analytics (`application.properties:6`) — the two that need it least. order/catalog/inventory/payment/auth/review/gateway don't set it.
**Impact:** a rollout/SIGTERM mid-`create()` kills the Tomcat worker → the saga `try/catch` never completes → PENDING order + orphaned reservation, no compensation.
**Fix (concept — graceful drain for zero-downtime rollouts):** `server.shutdown=graceful` + a sensible `spring.lifecycle.timeout-per-shutdown-phase` on every request service. Pair with readiness (F30) so the LB stops sending traffic before drain.

### F6 🟠 No circuit breaker on the payment & inventory critical path → cascade
**Where:** the only resilience4j-annotated method in the platform is `CatalogGateway.getProduct()` (a *read*). `OrderService.java:69` (inventory reserve) and `:73` (payment charge) have only timeouts; `InventoryClient`/`PaymentWebClient` have no CB/Retry/Bulkhead.
**Impact:** under a sustained payment/inventory outage every order pins a Tomcat thread for up to 5s; at ~40 req/s the 200-thread pool exhausts and **order-service stops serving everything**, incl. `GET /orders`. Classic cascading failure — on the one path you most want protected.
**Fix (concept — bulkhead + breaker + time limit on the money path):** copy the (correct) catalog resilience config to payment + inventory; add a `TimeLimiter` where the signature allows; keep load-shed exceptions out of the breaker's failure set (as catalog already does). Consider a thread-pool bulkhead (not semaphore) so a hang doesn't pin the request thread.

### F7 🟠 Money/stock endpoints authenticated but NOT authorized
**Where:** `payment SecurityConfig.java:14-22` + `PaymentController.java:23-31` (charge/refund, no role); `inventory SecurityConfig.java:14-22` + `InventoryController.java:23-31` (reserve/release, no role). Also `order OrderController.java:42-45` — `GET /orders` returns **all** users' orders to any authenticated user.
**Impact:** any valid user token (e.g. seeded `hemanth`/USER) can move money and mutate stock; broken object-level authorization leaks every order.
**Fix (concept — least privilege + object-level authz):** `@EnableMethodSecurity` + `@PreAuthorize` (service-role for charge/refund/reserve, or restrict to internal callers only); scope `GET /orders` to the caller's own `sub`. OWASP A01.

### F8 🟠 Auth signing key regenerated every boot → HA broken
**Where:** `auth-service/config/JwtKeyConfig.java:16-18` — `new RSAKeyGenerator(2048).keyID(UUID.randomUUID()).generate()`.
**Impact:** two auth replicas sign with different keys → a token from replica A fails against replica B's JWKS; every restart invalidates all live tokens. Cannot run >1 auth-service even after F3.
**Fix (concept — stable, shared, rotatable key material):** load one keypair from a mounted secret/keystore/KMS (same `kid` across replicas); support overlapping keys in JWKS for rotation. Longer-term: a real Authorization Server with a shared `JWKSource`.

### F9 🟠 Payment idempotency in-memory → double-charge + OOM
**Where:** `payment-service/service/PaymentService.java:22-23` — `ConcurrentHashMap charged` / `Set refunded`, never evicted.
**Impact:** across replicas the double-charge guard evaporates; lost on restart; maps grow unbounded → heap OOM at scale. It's the safety net for the whole saga.
**Fix (concept — externalized, bounded idempotency state):** move the dedup to Redis/DB keyed by a payment idempotency id, with TTL. (Even as a simulated ledger, keep the guard durable.)
**✅ Done (2026-07-19, Slice 3):** payment now owns a Postgres DB (`paymentdb`) with a `Payment` ledger — one row per order, `unique(order_id)`. `charge`/`refund` are `@Transactional` with a fast-path dedup + the unique constraint as the true exactly-once guard; the `ConcurrentHashMap`s are deleted. Durable, replica-safe, bounded. 5 unit tests green. (Chose the DB ledger over Redis+TTL — it's the system of record, so rows are kept, not expired.)

### F10 🟠 Outbox poller not multi-instance safe; unbounded fetch; no index
**Where:** `OutboxPoller.java:35-48` (`@Scheduled` on every replica, no lock; serial blocking `send().get()`); `OutboxRepository.java:13` (`findByPublishedFalse()` — no limit); `OutboxEvent.java:12-25` (no index on `published`, no purge).
**Impact:** the moment the DB is shared (F3), every replica publishes every event → duplicate storm. A broker backlog loads the whole table into heap every 2s → OOM. Table grows forever → poll query degrades to full scan.
**Fix (concept — single-writer relay + bounded work + retention):** `SELECT … FOR UPDATE SKIP LOCKED` (or ShedLock/leader election); `LIMIT` the batch; index `published`; purge/archive published rows. Keeps the (correct) atomic capture + message key.

### F11 🟠 Gateway rate-limit throttles all browse to ~10 req/s; `/auth`+`/orders` unlimited
**Where:** `api-gateway/RateLimitFilter.java:40` (only `/products`), `:62-64` (key = principal, but `GET /products/**` is `permitAll` → principal is null → key collapses to constant `"anonymous"`); limit default 10 (`application.properties:36`).
**Impact:** the entire planet's catalog browsing shares one ~10 req/s bucket → 429 storms on the hottest path; meanwhile login-flood/order-spam are unthrottled.
**Fix (concept — correct keying + per-route quotas):** key anonymous traffic by client IP (or an issued anon token); segment quotas per route; raise the browse limit; add limits to `/auth/login` and `/orders`.

### F12 🟠 Saga executor rejection not handled → corrupt state under load
**Where:** `SagaExecutorConfig.java:15-24` (core4/max8/queue50, default `AbortPolicy`); `OrderService.java:68-70` `runAsync` throws `RejectedExecutionException`, which is **not** in the catch at `:74`.
**Impact:** on pool saturation → 500 + committed PENDING order + already-submitted partial reservations, **no compensation**. Load shedding here corrupts state.
**Fix (concept — deliberate rejection policy + shed cleanly):** catch `RejectedExecutionException` and route it through the same compensation/fail path; choose a rejection policy intentionally (`CallerRunsPolicy` for backpressure vs a clean 503). Tie into F13.

### F13 🟠 Compensation failures swallowed with no retry
**Where:** `OrderService.java:76-91` — a failed `release`/`refund` is logged `"MANUAL RECOVERY NEEDED"` and dropped.
**Impact:** if inventory/payment is down during rollback, the stock leak / un-refunded charge is silent and permanent.
**Fix (concept — compensation is itself a durable, retried action):** enqueue a compensation task (outbox/DB row) the reconciler (F4) retries idempotently; alert if it exceeds N attempts. No best-effort-and-forget on money/stock.

### F14 🟠 Failed order poisons its idempotency key
**Where:** `order-service/service/IdempotencyOrderService.java:26-48` — the `IdempotencyRecord` is committed before `create()`; on failure it persists with `orderId=null`, so any retry with that key hits `replay()` → permanent `409 "still processing"`.
**Impact:** a transient failure makes that idempotency key unusable forever; the failure result is never returned.
**Fix (concept — record terminal outcome, allow safe retry):** store the terminal result (success id or failure) against the key; on replay return it; allow retry when the prior attempt didn't reach a committed side effect. Distinguish "in progress" from "failed, retryable."

### F15 🟠 Tracing dies at the Kafka boundary; only 4/10 services; no collector
**Where:** Brave bridge + Zipkin exporter only in order/payment/catalog/inventory poms. Gateway, auth, review, **both Kafka consumers** have no tracing lib. No `spring.kafka.*.observation-enabled` anywhere; `OutboxEvent` carries no `traceparent`. **No Zipkin/OTLP endpoint configured** → defaults to `localhost:9411` (dropped in a real deploy). Sampling hardcoded `1.0`.
**Impact:** you cannot follow order #12345 end-to-end — the trail goes cold the moment it becomes a Kafka event, and even the 4-service HTTP trace isn't exported anywhere.
**Fix (concept — one trace, edge-to-consumer):** add the tracing bridge to all 10 services incl. the gateway (generate the trace at the edge); enable Kafka observation so B3/W3C headers ride the message (or stamp `traceparent` into the outbox row and restore it in the consumer); configure a reachable collector (OTLP → Tempo/Zipkin); set sampling per profile (F24).

### F16 🟠 Prometheus on order-service only; zero business metrics
**Where:** `micrometer-registry-prometheus` only in `order-service/pom.xml`; `/actuator/prometheus` exposed only there. Grep for `MeterRegistry`/`Counter`/`Timer`/`@Timed` across `src/main` = 0 hits.
**Impact:** 9 services unscrapable; nothing alertable on payment-failure rate, reservation conflicts, saga compensations, or orders/sec.
**Fix (concept — RED/USE + domain metrics):** Prometheus registry in every service; add custom counters/timers on the money/stock/saga paths (analytics already counts into Redis — also expose it as a meter). These become your SLO alerts.

### F17 🟠 Unbounded list queries / no default pagination → OOM as data grows
**Where:** catalog `ProductService.java:129` (`findAll(spec)`) + `:48` (`findByNameIgnoreCase`) — paginated variants are separate opt-in endpoints; review `ReviewRepository.java:17-18` (all reviews+images for a product); order `OrderRepository.java:15-19` (`findAllWithItems()`); audit `AuditLogService.java:28-30` (`findAll()`); outbox `findByPublishedFalse()` (F10).
**Impact:** every default list loads a full, growing table into memory → latency + OOM.
**Fix (concept — pagination by default, bound everything):** `Pageable` on all list endpoints (make the paged path the default, not opt-in); cap audit/outbox scans; add DB indexes for the filter columns.

### F18 🟡 Gateway has no downstream response timeout or circuit breaker
**Where:** `api-gateway/application.properties` — no `spring.cloud.gateway.httpclient.response-timeout`, no CB filter (only `RateLimitFilter`).
**Impact:** Netty's default response timeout is effectively unbounded → a hung downstream pins gateway connections/event-loop capacity under load.
**Fix (concept — bound + shed at the edge):** set `connect-timeout` + `response-timeout`; add a Spring Cloud Gateway `CircuitBreaker` filter (resilience4j) per route with a fallback.

### F19 🟡 Gateway rate-limiter fails closed on Redis error; non-atomic INCR/EXPIRE
**Where:** `RateLimitFilter.java:56-59` (`onErrorResume` → 503 for all `/products`), `:46-47` (INCR then EXPIRE — a crash between leaves a TTL-less key).
**Impact:** an Upstash blip = full catalog-browse outage; a leaked key throttles a user permanently.
**Fix (concept — decide the failure mode; make it atomic):** for public browse, fail *open* (or degrade) on limiter-backend errors; make INCR+EXPIRE atomic (Lua script / `SET NX PX` pattern / Redis rate-limit module). Pairs with F11.

### F20 🟡 Unauthenticated, non-idempotent gRPC reserve surface
**Where:** `inventory-service` `grpc.server.port=9090` (`application.properties:30`) runs `InventoryGrpcService` (`:22`) that mutates stock via the **naive, non-locked, non-idempotent** `reserve(...)` (`InventoryService.java:29-40`), catches only `InSufficientStockException`, and sits **outside** the Spring Security filter (that guards only servlet :8082). No caller anywhere. `reservePessimistic()` is dead code.
**Impact:** latent unauthenticated stock-mutation endpoint; under contention it double-decrements on retry and leaks optimistic-lock errors as gRPC UNKNOWN.
**Fix (concept — secure or delete; one reserve path):** if kept, add a gRPC auth interceptor + route it through the locked+idempotent `reserve(orderId,productId,qty)`; otherwise delete the gRPC surface. Don't ship an unused, unguarded write path.

### F21 🟡 Analytics double-counts on redelivery
**Where:** `analytics-service/AnalyticsListener.java:27-34` — `hasKey` → `increment(orders)` + `increment(revenue)` run **before** the dedup `setIfAbsent`, and the two increments aren't atomic.
**Impact:** at-least-once redelivery (concurrency=3, rebalance, crash window) double-counts orders/revenue.
**Fix (concept — claim before effect, atomically):** make the dedup claim gate the increments (`setIfAbsent` first; only proceed if you won); or fold the whole update into one atomic Lua script. "Mark = completed," not "started."

### F22 🟡 Kafka durability + evolution + DLT gaps
**Where:** `KafkaTopicConfig.java:18` (`replicas=1` negates `acks=all`; 3 partitions × concurrency 3 = throughput ceiling); no schema registry (POJO+JSON with `use.type.headers=false` workaround); DLQ exists but **no `.DLT` consumer/drain/alert**.
**Impact:** a single broker loss drops acknowledged messages; a field rename silently breaks consumers; dead-lettered events accumulate unseen.
**Fix (concept — durability, contracts, closed DLQ loop):** `replicas≥3` + `min.insync.replicas=2` (real cluster); Avro/Protobuf + schema registry with compat rules; a DLT drain/reprocess job + alerting. (Enterprise roadmap Track D covers this.)

### F23 🟡 Kafka plaintext — no TLS/SASL/ACL
**Where:** all three Kafka services use `bootstrap-servers=localhost:9092`, no `security.protocol`.
**Impact:** unencrypted, unauthenticated broker traffic; any reachable client can read/write topics.
**Fix (concept — encrypt + authenticate + authorize the bus):** TLS + SASL, per-service credentials, topic ACLs. Pin trusted packages.

### F24 🟡 No `logback-spring.xml`; unstructured logs; sampling hardcoded
**Where:** none in any `src/main`; default console pattern only; `TRACE_SAMPLING:1.0` default with no prod profile.
**Impact:** no JSON/structured output for Loki/ELK, no rolling files, no per-env levels; 100% trace sampling is expensive in prod.
**Fix (concept — structured, per-env logging):** `logback-spring.xml` with JSON encoder + `<springProfile>` blocks; sampling low in prod, 1.0 in dev; async appender. Ties into F26.

### F25 🟡 PII logged at INFO
**Where:** `order-service/aspect/LoggingAspect.java:23,30` (all method args + return values → customer names, line items, prices); `notification/NotificationListener.java:31-32` (customerName); `OrderPersistence.java:37` (customerName into the event payload the consumer logs).
**Impact:** PII in aggregated logs — GDPR/compliance exposure.
**Fix (concept — scrub/allow-list what you log):** never blanket-log args/returns; log ids not payloads; add a redaction layer; drop the exception-at-INFO in `GlobalExceptionHandler.java:45`.

### F26 🟡 No dev/prod profiles; insecure-by-default fallbacks
**Where:** one `application.properties` per service using `${ENV:default}`, where the *defaults* are dev-exposure: `H2_CONSOLE:true`, `SHOW_SQL:true`, `TRACE_SAMPLING:1.0`, seed creds, H2 file. No `application-{profile}.yml` anywhere.
**Impact:** a plain `java -jar` with no env is fully exposed; you can't promote one artifact dev→prod safely.
**Fix (concept — one artifact, many environments; safe by default):** `dev`/`prod` profiles; production defaults that are *closed* (console off, sample low, no seed creds, real DB). Config externalized (already is via `${ENV}` — keep that).

### F27 🟡 Inconsistent error handling on 3 services; mismaps; cause leak
**Where:** catalog/review/auth have **no** `@RestControllerAdvice` (fall through to Whitelabel, not `ProblemDetail`). order `GlobalExceptionHandler.java:49-52` + inventory `:33-36` map `IllegalStateException → 404` (a server-invariant → wrong status) and echo its message; order `OrderFailedException` (`:60-64`) leaks the downstream cause string into the client 409.
**Impact:** inconsistent API error shape; misleading statuses; internal detail leak.
**Fix (concept — one error contract, safe messages):** shared `ProblemDetail` advice across all services (candidate for a `mercato-framework` starter); map server invariants to 500, lock conflicts to 409; log full detail server-side, return a safe generic message.

### F28 🟡 Inventory contention returns 500, no retry on reserve
**Where:** pessimistic `findByProductIdForUpdate` (`InventoryService.java:44`); an H2 lock-wait timeout → `CannotAcquireLockException` → generic handler → **500** (only optimistic maps to 409). No retry on reserve anywhere.
**Impact:** transient contention looks like a server bug and fails a legitimate order a short retry would save; a timeout-vs-commit race can also orphan a reservation (compensation `release` no-ops before the row is visible).
**Fix (concept — map lock conflicts → retryable, and retry):** map `CannotAcquireLock`/lock timeouts → 409/`Retry-After`; add a bounded retry-on-conflict on the reserve path; guard the timeout-vs-commit race with the reservation record (make reserve fully idempotent so a late commit is reconciled).

### F29 🟡 Trace/MDC not propagated to async threads
**Where:** `SagaExecutorConfig.java:23` wraps only `DelegatingSecurityContextExecutor` (security, not trace); `AsyncConfig.java:16-25` audit pool has no decorator and no `AsyncUncaughtExceptionHandler`.
**Impact:** parallel-reserve spans + audit writes detach from the request trace; async audit exceptions are swallowed.
**Fix (concept — context-propagating executors):** wrap with a tracing-aware `TaskDecorator` (Micrometer context propagation) in addition to security; add an `AsyncUncaughtExceptionHandler`.

### F30 🟡 Actuator/Swagger exposure; probes on only 2/10 services
**Where:** unauthenticated `/actuator/prometheus` on order (`application.properties:34` + `SecurityConfig.java:22` permitAll); Swagger `permitAll` on all resource servers; `health.probes.enabled=true` only on notification+analytics; no custom readiness indicators anywhere. (Positive: `show-details=when-authorized`, no `env`/`heapdump`/`shutdown` exposed.)
**Impact:** metrics/API-surface disclosure; K8s can't gate traffic on real readiness for 8/10 services.
**Fix (concept — least exposure + real probes):** gate metrics/actuator behind auth or a separate management port; liveness/readiness on all services with readiness indicators for DB/Kafka/Redis; decide Swagger exposure per profile.

### F31 🟡 Stale framework + no dependency scanning
**Where:** ~~Spring Boot **3.3.5** (Oct 2024, `pom.xml:11`)~~; `.github/workflows/ci.yml` is build+test only — no OWASP Dependency-Check/Dependabot/CodeQL/Snyk/Trivy, no `dependabot.yml`.
**Impact:** unpatched CVEs with no detection.
**Fix (concept — patch cadence + build-time scanning):** bump to a current release; add Dependabot + a CVE scan gate + image scan (F-deploy).
**✅ Partly done (2026-07-19):** upgraded to **Spring Boot 3.5.15** + **Spring Cloud 2025.0.3 (Northfields)** — 14/14 modules build (only 2 deprecation warnings left: `@MockBean`→`@MockitoBean`, `Specification.where`). ⏳ Still open: Dependabot + OWASP/Trivy CVE-scan gate in CI.

### F32 ⚪ No cache on hot reads
**Where:** catalog by-id is cached (`ProductService.java:42`, 60s) but **list/search aren't** (`:47,:113`); review has no cache + no rating aggregate (reloads full review set per product page).
**Impact:** read-heavy paths hit the DB every request — the first thing to bite once F3 lets you scale reads.
**Fix (concept — cache-aside on hot reads + a read model):** cache list/search results (short TTL, evict on write); precompute/cache a review-rating aggregate (feeds the CQRS read model in the roadmap).

### F33 ⚪ Dead code / dual stacks
**Where:** Feign `PaymentClient.java` unused (order uses `PaymentWebClient` — two HTTP stacks in one service); `reservePessimistic()` unused; gRPC surface unused (F20).
**Fix:** delete unused paths — removing scaffolding is part of "done."

### F34 ⚪ Single Upstash Redis is a shared SPOF
**Where:** one Redis backs gateway limiter + catalog cache + notification/analytics idempotency.
**Impact:** its outage degrades limiter (F19), cache, and dedup at once.
**Fix (concept — HA + blast-radius):** managed HA Redis (or separate instances per concern); make each dependency degrade gracefully.

### F35 ⚪ Eureka eviction + no retry on 2/3 edges
**Where:** default ~90s eviction; only catalog edge has retry.
**Impact:** in-flight calls to a just-died replica fail rather than reroute.
**Fix:** `prefer-ip-address` + tuned lease/eviction; retry (bounded, idempotent) on the inventory/payment edges (overlaps F6).

### F36 ⚪ Catalog `PUT update()` skips validation
**Where:** `catalog ProductController.java:62` omits `@Valid`.
**Fix:** add `@Valid`; audit DTO validation coverage while you're there.

### F37 ⚪ Gateway doesn't strip client identity headers
**Where:** `ApiGatewayApplication.java:20-27` — `X-User`/identity headers pass through un-stripped. Low risk today (identity = signed JWT, and the limiter now keys off the validated principal), but a foot-gun if any service ever trusts `X-User`.
**Fix:** strip client-supplied identity headers at the edge; derive identity only from the validated token.

### F38 ⚪ Only the happy-path event exists
**Where:** `events` module has `OrderPlaced` only — no `OrderFailed`/`OrderCancelled` despite the saga's failure paths.
**Fix (concept — model failure as first-class events):** emit failure/cancel events (via the outbox) so analytics/notification see the full lifecycle; a step toward the choreography track.

### F39 ⚪ Misleading stale comments & docs
**Where:** `JwtService.java:19-23` Javadoc claims "HS256 … same secret" but the code is RS256+JWKS; `PRODUCTION-READINESS-REVIEW.md` (2026-07-09) describes the pre-auth design. 
**Fix:** correct the comment; this file supersedes the old review — keep it as history but don't present it as current.

---

## Cross-reference — your questions → findings

| Your question | Findings |
|---|---|
| **Millions of requests / scale hot services** | F3, F11, F8, F9, F10, F17, F32, F34, F6 (cascade) |
| **Error handling on failure** | F4, F5, F6, F12, F13, F14, F27, F28, F18, F19 |
| **Traceability & observability** | F15, F16, F24, F25, F29, F30 |
| **Best practices of real production** | F2, F7, F23, F26, F31, F33, F36, F37, F39 |
| **Kafka / EDA real vs nominal** | F10, F21, F22, F23, F38 (real tool, one pipeline; EDA breadth nominal) |
| **Multithreading real vs nominal** | Real — bounded pools + parallel reserve + security propagation; gaps only F12, F29 |
| **Synchronous communication** | Real backbone — F6, F18, F35 are the gaps |

## What's genuinely strong (keep — and say in interviews)
Real Eureka client-side LB (no hardcoded hosts) · correct RS256/JWKS auth with edge validation +
downstream re-validation + JWT propagation across the async reserve threads · saga that avoids
connection pinning and commits PENDING first · timeout-aware compensation with refund · DB-backed
idempotency key · pessimistic lock + reservation-record idempotency · transactional outbox with
atomic capture + message key + `acks=all`/idempotence · **resilience4j configured textbook-correct** ·
real bounded thread pools with genuine parallel line-item reservation · clean SLF4J (no `System.out`) ·
conservative actuator exposure. The honest framing — *"production-shaped, not production-proven"* —
is a stronger interview line than "production-ready."
