# E-Commerce Microservices — Learn-by-Building Roadmap

> **What this is:** the full plan to evolve `order-service` into a distributed e-commerce
> platform, used as a **portfolio piece** while learning every concept **hands-on**.
> **Dual goal:** (1) touch each technology with my own hands so I understand the trade-offs,
> (2) end with a coherent, deployed, documented system I can show.

---

## 0. How we work (the method)

- **One tiny concept at a time.** Each phase is broken into small steps. We finish and
  understand a step before moving to the next.
- **I write the code.** Claude gives me the *goal + API surface + algorithm/steps + what to
  assert*. I type it. Claude reviews and explains the **"why"**.
- **I ask "why" freely.** Whenever I hit a decision (which client? which pattern? why this
  config?), I ask, and we log the answer in the **Decisions Log** at the bottom.
- **Deliberate variety for learning.** I am *intentionally* using a different communication
  style per service (Feign / gRPC / WebClient / RestClient / Kafka) so I learn all of them.
  This is a learning choice, not a production default — and the README will say so honestly
  (see *Honesty note* below). Each one is still placed where it's at least *plausible*.

**Honesty note for the README (turns breadth into a strength):**
> "This is a reference/learning implementation. I deliberately use several communication
> styles (Feign, gRPC, WebClient, Kafka) across services to demonstrate each and its
> trade-offs. A production system would standardize on fewer."

---

## 1. The domain

An **e-commerce order platform**. A customer browses a catalog, places an order; the system
reserves stock, takes payment, confirms the order, and notifies the customer — across
independent services that each own their data.

This domain was chosen because it *naturally* needs everything on the learning list:
read-heavy catalog → caching; place-order spanning 3 services → saga; notifications →
events; hot stock path → gRPC + locking. Nothing is bolted on.

---

## 2. Target architecture

```
                          ┌──────────────┐
                          │   Angular     │   storefront + admin (TypeScript)
                          └──────┬───────┘
                                 │  REST + GraphQL  (JWT in header)
                          ┌──────▼───────┐
                          │ API Gateway  │   Spring Cloud Gateway
                          │  - routing   │   - JWT auth at the edge
                          │  - rate-limit (Redis)
                          └──────┬───────┘
       ┌───────────┬────────────┼────────────┬──────────────┐
  ┌────▼────┐ ┌────▼─────┐ ┌────▼──────┐ ┌───▼─────┐ ┌──────▼───────┐
  │  Order  │ │ Catalog  │ │ Inventory │ │ Payment │ │ Notification │
  │ (saga   │ │ (GraphQL │ │ (gRPC     │ │         │ │ (Kafka       │
  │  orch.) │ │  + Redis)│ │  server)  │ │         │ │  consumer)   │
  └────┬────┘ └────┬─────┘ └────┬──────┘ └────┬────┘ └──────┬───────┘
       │           │            │             │             │
       └─ each service owns its own Postgres DB ─┘          │
       │                                                    │
   ════╪════════════ Kafka (event backbone) ════════════════╪════
       OrderPlaced / StockReserved / PaymentCompleted / OrderConfirmed
```

### Communication map (sync vs async — chosen per interaction)

| Interaction | Style | Mechanism | Why this one |
|---|---|---|---|
| Order → Catalog (price/details) | **sync** | **Feign** | Need answer now; declarative + integrates with LB/resilience |
| Order → Inventory (reserve stock) | **sync** | **gRPC** | Latency-sensitive hot path; binary + HTTP/2; learn contract-first |
| Order → Payment (charge) | **sync** | **WebClient** | Slow external-style call → non-blocking; my on-ramp to Reactive |
| Reporting/Admin → others | **sync** | **RestClient** | Simple one-off read; modern RestTemplate replacement |
| Order → Notification / Analytics | **async** | **Kafka** | Fire-and-forget, eventual consistency, fan-out to many consumers |

**Rule of thumb learned:** *sync when the caller needs the result to proceed; async when it
can happen eventually.* Don't make everything sync (cascading failures) or everything async
(can't answer the user now).

### Data ownership
- **DB-per-service** (each service has its own Postgres schema/instance). No shared database.
- Cross-service consistency = **events + saga**, never a distributed SQL transaction (no 2PC).

---

## 3. Technology inventory (what & why)

| Area | Tech | Where used | Why it's there |
|---|---|---|---|
| Framework | Spring Boot 3.x | all services | baseline |
| Language | Java 21 (bump from 17) | all services | **virtual threads** for the threading module |
| Persistence | PostgreSQL | per service | real DB (replaces H2) |
| Migrations | Flyway | per service | versioned schema (replaces `ddl-auto`) |
| Cache / lock / rate-limit | Redis (+ Redisson) | Catalog cache, gateway rate-limit, Inventory lock | read speed, distributed lock, throttling |
| Sync comms | Feign, gRPC, WebClient, RestClient | see comm map | learn all four + when to use each |
| Async comms | Apache Kafka | event backbone | event-driven arch, saga, fan-out |
| Distributed txn | Saga (orchestration) + Outbox | Order placement | atomicity across services without 2PC |
| API edge | Spring Cloud Gateway | gateway | routing, edge auth, rate-limit |
| Service discovery | Eureka (local) → K8s DNS (deploy) | all | client-side load balancing |
| Resilience | Resilience4j | sync calls | retry, circuit breaker, bulkhead, rate limiter |
| API styles | REST, GraphQL | Catalog GraphQL | flexible client queries |
| Observability | Micrometer + Prometheus + Grafana | all | metrics |
| Tracing | Micrometer Tracing + OpenTelemetry + Tempo/Zipkin | all | one request traced across services |
| Logging | structured JSON + MDC correlation id → Loki | all | searchable cross-service logs |
| Containers | Docker (multi-stage) + docker-compose | all | local full stack |
| Orchestration | Kubernetes + Helm | all | deploy, scale, config/secrets |
| Frontend | Angular + TypeScript | storefront/admin | full-stack story |
| CI/CD | GitHub Actions | repo | build/test/push images |
| Testing | JUnit5, MockMvc, Testcontainers, spring-security-test | per service | prove it works |

**Stretch / optional (mark done only if time):** Debezium CDC, Keycloak/OAuth2 + JWT
propagation, Pact contract testing, Istio service mesh, chaos testing.

---

## 4. The phases

Each phase template: **Goal → New concepts → Build → Why/talking points → Definition of Done.**
**Test as you build** (don't save testing for the end). **🏁 = portfolio-worthy milestone.**

### Phase 0 — Harden the seed (order-service becomes production-shaped)
*You can't have microservices until one service is solid, on a real DB, and containerized.*
- **New concepts:** externalized config, profiles, migrations, ops endpoints, RFC-7807, containerizing.
- **Build:**
  - [x] Fix the JWT `isValid()` bug (it never actually validates the token). ✅ 2026-06-21
  - [x] DB: use **H2 file-mode** (persistent, free, zero-install) now; optional one-line swap to free **Neon** Postgres later for the portfolio. (No local Docker — see Decisions Log.) ✅ 2026-06-21
  - [x] **Flyway** migrations; set `ddl-auto=validate`. ✅ 2026-06-21
  - [x] Externalize config: `@ConfigurationProperties` for JWT secret/expiry. ✅ 2026-06-21 (dev/prod profiles = optional later polish)
  - [x] **Actuator** (`/health`, `/metrics`, `/info`). ✅ 2026-06-21
  - [x] `GlobalExceptionHandler` → **`ProblemDetail`** + catch-all; add `@Valid` to `OrderDtos`. ✅ 2026-06-21 (add `detail.setProperty("errors", fieldErrors)` to the validation handler)
  - [~] **Dockerfile** (multi-stage) for order-service. ⏸ deferred — no local Docker; will build images in CI/Codespaces at Phase 7.
- **Why:** statelessness + real DB + container are the preconditions for everything after.
- **DoD:** order-service runs on Postgres in Docker, config externalized, actuator up, errors clean.

### Phase 1 — Become microservices (split + gateway + sync comms)
- **New concepts:** service decomposition, DB-per-service, declarative HTTP, gateway, discovery, client-side LB.
- **Build:**
  - [x] Repo layout: **separate standalone projects** for now (parent reactor pom at Phase 9). ✅ 2026-06-21
  - [x] Extract **Catalog service** (its own DB) from order-service. ✅ 2026-06-21
  - [x] Order → Catalog via **Feign** (+ `OrderItem` snapshot; productId is a plain column, no cross-service FK). ✅ 2026-06-21
  - [x] **Eureka** service registry (`discovery-service`, port 8761). catalog + order register; Feign `CatalogClient` switched from hardcoded `url` → name-based discovery + client-side LB. Self-preservation disabled for dev. ✅ 2026-06-22
  - [ ] **Spring Cloud Gateway** in front; route `/orders`, `/products`.
- **Why:** this is the exact moment a monolith becomes a distributed system; the gateway gives one entry point and one place for edge concerns.
- **DoD:** two services + gateway run together; an order created through the gateway fetches product data from Catalog over Feign.

### Phase 2 — Caching + Resilience
- **New concepts:** cache-aside + invalidation, the four Resilience4j patterns, where each applies.
- **Build:**
  - [x] **Redis** cache-aside on Catalog reads (`@Cacheable` on `findById`), **evict on write** (`@CacheEvict` on update/delete). Learned in-memory first, then swapped provider to **Upstash Redis** (free tier, Mumbai) with **zero code change** — only deps/config. TTL 60s + JSON value serialization; creds externalized via env/VM args (`${REDIS_*}`). ✅ 2026-06-22
  - [x] **Resilience4j** on the order→catalog call: circuit breaker + retry + bulkhead + rate limiter, all on `CatalogGateway.getProduct` (a separate bean — proxy/self-invocation rule). Fallback maps failures → domain exceptions (404 vs 503). ✅ 2026-06-22
  - [x] Redis-backed **rate limiting at the gateway**: `RequestRateLimiter` filter on the `/products` route, backed by **reactive** Redis (Upstash), keyed per user via a `userKeyResolver` (`X-User` header, else `anonymous`). `burstCapacity=10`, `replenishRate=5/s`. Verified: 40-req burst → 10×200 → 429 → 5×200 (refill) → sustained 429 = textbook token bucket. ✅ 2026-06-22 (commit 372b222)
- **Why:** caching cuts DB load on read-heavy data; resilience stops one slow/broken service from taking down its callers. Invalidation is where correctness lives. Gateway rate-limiting is **traffic control** (protect downstream from overload/abuse), distinct from the Resilience4j fault-tolerance patterns.
- **DoD:** ✅ Catalog reads hit Redis; killing Catalog trips the breaker (Order degrades, doesn't hang); cache is consistent after an update; gateway throttles per-user with 429s.

### Phase 3 — Event-Driven + Kafka 🏁
- **New concepts:** producer/consumer, topics/partitions/consumer-groups, eventual consistency, sync-vs-async revisited.
- **Build:**
  - [x] **Kafka** running locally in **KRaft mode** (no Docker; `D:\Softwares\kafka` 3.9.1, `config/kraft/server.properties`, `log.dirs` repointed to a Windows path). Topic `order-events` (1 partition, RF 1). ✅ 2026-06-22
  - [x] Order publishes **`OrderPlaced`** after save — `KafkaTemplate` + `JsonSerializer`, fire-and-forget. ✅ 2026-06-22
  - [x] **notification-service** consumes via `@KafkaListener` → logs a mock notification. Verified live end-to-end (POST /orders → Kafka → notification). ✅ 2026-06-22 (commit e6d9fc7)
  - [x] Second consumer group (**analytics-service**) on the same topic → demonstrates **fan-out** (both services get every event independently). Aggregates revenue/count. Also showed offsets are per-group + durable (existing group resumes; new group replays from earliest). ✅ 2026-06-22 (commit ff61787)
- **Why:** decouples side-effects from the request path; multiple consumers react independently.
- **DoD:** ✅ placing an order produces an event; notification-service reacts without order-service knowing about it. **🏁 Credible portfolio piece reached.** (Analytics fan-out would extend it but isn't required.)

### Phase 4 — Distributed transactions: Saga + Inventory + Payment + Threading
- **New concepts:** saga orchestration vs choreography, transactional outbox, idempotency, distributed locking, optimistic vs pessimistic locking, race conditions, virtual threads.
- **Build:**
  - [x] Add **Inventory** + **Payment** services. **inventory-service** (port 8082, own H2+Flyway, Eureka; `InventoryItem` w/ `@Version`; `reserve` no-oversell→409, `release` compensates) ✅ 2026-06-22 (commit 0092bbf). **payment-service** (port 8083, stateless gateway sim, Eureka; `charge` declines >limit→402, `refund` compensates) ✅ 2026-06-23 (commit 879e4ef).
  - [x] **Saga (orchestration)** for order placement: `OrderService.create()` runs reserve stock → charge payment → confirm; on a Feign failure it **compensates** (releases every reservation that succeeded) and throws `OrderFailedException`→409. Local `@Transactional` rolls back the order INSERT; the remote reservation is undone explicitly via `release()`. Verified live across 6 services (normal→201; >limit→402 decline→stock released→rolled back→409). ✅ 2026-06-23 (commit c931987)
  - [x] **Transactional Outbox** so DB-commit and event-publish can't diverge. `create()` writes the order **+** an `outbox` row in ONE `@Transactional` DB tx (no dual-write); an `OutboxPoller` (`@Scheduled(fixedDelay=2000)` + `@Transactional`) reads `findByPublishedFalse()`, sends to Kafka, **waits for the ack (`.get()`)** *before* `markPublished()` (dirty-checked UPDATE) → at-least-once. Verified live incl. the **Kafka-down test**: order still 201, row parked `published=false`, drains to `true` on Kafka restart, zero loss. ✅ 2026-06-23
  - [x] **Idempotency keys** (Redis) on consumers. Both notification + analytics dedupe via `StringRedisTemplate.setIfAbsent("<consumer>:OrderPlaced:<orderId>", "1", 24h TTL)` — **per-consumer key prefix** so each processes the event once independently; `true`→first time→process, `false`→duplicate→skip (`Boolean.FALSE.equals`, null-safe). Handles the at-least-once dups the outbox can produce. Verified via kafka-console-producer (duplicate orderId skipped by both, new id processed). ✅ 2026-06-24
  - [ ] **Distributed lock** (Redisson) on stock reservation → **concurrency/threading module**: reproduce a race condition with concurrent orders, then fix it with the lock; compare with optimistic (`@Version`) vs pessimistic DB locking; try `CompletableFuture` fan-out and **virtual threads**.
- **Why:** the heart of distributed systems — getting "all-or-nothing" and "exactly-once-ish" without a shared transaction. The outbox is *the* reliability pattern; idempotency is *the* event-consumer pattern.
- **DoD:** an order that fails at payment rolls back stock via compensation; duplicate events don't double-charge; two concurrent orders for the last item don't oversell.

### Phase 5 — Protocol variety: gRPC + GraphQL + WebClient
- **New concepts:** gRPC/protobuf/HTTP-2/streaming, contract-first design, GraphQL schema/resolvers + its N+1, reactive basics.
- **Build:**
  - [ ] Convert **Order → Inventory** reserve-stock to **gRPC** (write the `.proto`, codegen stubs).
  - [ ] Add a **GraphQL** endpoint to Catalog (flexible product queries).
  - [ ] **Order → Payment** via **WebClient** (reactive on-ramp; ties to interview roadmap's Reactive end).
- **Why:** feel the difference — binary contract-first vs JSON, client-shaped queries vs fixed endpoints, blocking vs non-blocking.
- **DoD:** stock reservation flows over gRPC; a GraphQL query returns only requested fields; payment call is non-blocking.

### Phase 6 — Observability (the three pillars)
- **New concepts:** metrics vs traces vs logs, distributed tracing, correlation IDs.
- **Build:**
  - [ ] **Metrics:** Micrometer → **Prometheus** → **Grafana** dashboard.
  - [ ] **Tracing:** Micrometer Tracing + **OpenTelemetry** → **Tempo/Zipkin**; trace one order across all services.
  - [ ] **Logs:** structured JSON + **MDC correlation id** → Loki (optional ELK).
- **Why:** in a distributed system you can't debug by reading one log file; you need a trace that follows the request and metrics that show health.
- **DoD:** one Grafana dashboard; clicking a trace shows the order's path gateway→order→inventory→payment with timings.

### Phase 7 — Containerize & orchestrate
- **New concepts:** containers, orchestration, scaling, config/secrets, ingress.
- **Build:**
  - [ ] Dockerfile per service (multi-stage); **docker-compose** for the whole stack locally.
  - [ ] **Kubernetes:** Deployments (replicas), Services, ConfigMaps, Secrets, Ingress, HPA (autoscaling).
  - [ ] **Helm** charts.
- **Why:** demonstrates the "deploy N instances behind a load balancer, scale on load" story end-to-end.
- **DoD:** `docker-compose up` runs everything; `kubectl apply`/`helm install` deploys to a local cluster (kind/minikube) and scales a service.

### Phase 8 — Frontend: Angular + TypeScript
- **New concepts:** Angular components/services/routing, TS, calling REST + GraphQL, JWT flow. *(Plugs into the separate Angular learning track.)*
- **Build:**
  - [ ] Storefront (browse catalog, place order) + simple admin.
  - [ ] Talk to the **gateway** (REST + GraphQL); JWT login/refresh.
- **DoD:** a user can log in, browse, and place an order through the UI, all the way to a confirmation.

### Phase 9 — Portfolio polish
- **Build:**
  - [ ] Top-level **README** with the architecture diagram (the money shot) + the honesty note.
  - [ ] Per-service READMEs.
  - [ ] **CI/CD:** GitHub Actions (build, test, push images).
  - [ ] Seed/demo data + a scripted demo (and a short GIF/screenshots).
  - [ ] **Decisions & trade-offs** doc (why each tech, what you'd change for prod).
- **DoD:** a stranger can clone, run, and understand it in 10 minutes.

---

## 5. Cross-cutting (woven through every phase, not a phase)

- **Testing:** test each piece as you build it — `@WebMvcTest`/`@DataJpaTest` slices,
  `spring-security-test` for auth, **Testcontainers** for real Postgres/Kafka/Redis in tests.
- **Security:** JWT at the gateway; propagate identity to downstream services; optional
  Keycloak/OAuth2 later.
- **Config:** externalized everywhere from Phase 0; centralized config (Spring Cloud Config) optional.

---

## 6. Progress tracker

| Phase | Title | Status | Notes |
|---|---|---|---|
| 0 | Harden the seed | ☑ done | JWT fix, H2 file-mode, Flyway, Actuator, @ConfigurationProperties, ProblemDetail (2026-06-21). Dockerfile deferred to Phase 7. |
| 1 | Microservices split + gateway | ☑ done | Catalog extracted + Order→Catalog via Feign (2026-06-21); Eureka discovery (name-based Feign); Spring Cloud Gateway routing (2026-06-22) |
| 2 | Caching + resilience | ☑ done | Resilience4j (CB+retry+bulkhead+RL) + Redis cache-aside (Upstash, TTL, evict-on-write) + **gateway rate-limiter** (token bucket, verified 429s) — all 2026-06-22 |
| 3 | Event-driven + Kafka 🏁 | ☑ done | order publishes `OrderPlaced` → Kafka (KRaft, local) → notification-service `@KafkaListener` consumes; verified live (2026-06-22). Analytics fan-out optional. **Portfolio milestone reached.** |
| 4 | Saga + outbox + threading | ◧ in progress | inventory + payment + **saga** (c931987) + **outbox** (6daaf12, Kafka-down test) done; **idempotency done** — both consumers dedupe via Redis `setIfAbsent` (2026-06-24). Last: distributed lock/concurrency |
| 5 | gRPC + GraphQL + WebClient | ☐ | |
| 6 | Observability | ☐ | |
| 7 | Docker + Kubernetes | ☐ | |
| 8 | Angular frontend | ☐ | |
| 9 | Portfolio polish | ☐ | |

*Status legend: ☐ not started · ◧ in progress · ☑ done*

---

## 7. Decisions Log (fill as we go)

> Format: **Date — Question — Decision — Why.** This is the "why I chose X" record that
> makes the portfolio (and interviews) strong.

- _2026-06-21 — Communication style per service?_ — Deliberately vary (Feign/gRPC/WebClient/RestClient/Kafka) — to learn each hands-on; each placed where it's plausible; README states this honestly.
- _2026-06-21 — Saga style?_ — **Orchestration** — easier to trace/debug/explain than choreography.
- _2026-06-21 — Real DB?_ — **Postgres** (H2 was demo-only) — persistence + realistic behavior.
- _2026-06-21 — Where does it run? (cost = $0 hard constraint)_ — **Local Spring apps (Maven); free infra only.** DB = **H2 file-mode** locally for now (zero install/admin/cost); swapping to free **Neon** Postgres is a ~3-line change if wanted for the portfolio later. Redis/Kafka (Phases 2/3) → **free cloud tiers** (Upstash / Redis Cloud — no credit card) since H2 can't replace them and there's no local Docker. Kubernetes (Phase 7) → free/cloud cluster. ⚠️ never commit any cloud credential to the public repo — externalize via env vars.
- _2026-06-21 — Repo layout (revised)_ — **Separate standalone Spring Boot projects** per service for now (each its own port), NOT a Maven reactor yet — avoids Eclipse re-import friction and keeps the working order-service untouched. Wrap in a parent reactor pom at Phase 9 for portfolio polish.
- _2026-06-22 — Eureka now, or skip to K8s DNS?_ — **Eureka now.** Discovery is a core concept I want to feel hands-on (registration, heartbeats, client-side LB); K8s DNS at Phase 7 will *replace* it. Built `discovery-service` (Eureka server) before the gateway so the gateway is born using discovery (no rework).
- _2026-06-22 — Eureka self-preservation banner in dev?_ — **Disabled (`enable-self-preservation=false`) in dev only.** On a 2-instance laptop the heartbeat count sits below threshold and trips the "EMERGENCY" banner falsely; disabling lets dead services get evicted promptly. **Leave it ON (default) in prod** — there it guards against mass eviction during a network partition.
- _2026-06-22 — Where do resilience patterns live?_ — **Caller-side, on a separate `CatalogGateway` bean** wrapping the Feign client. Why a separate bean: Resilience4j works via Spring AOP proxies, so an `@CircuitBreaker` method called from the *same* bean (self-invocation) is bypassed. Why caller-side: a circuit breaker protects the *caller* from a dead dependency — there's no code running in catalog to "break" when catalog is down. (Server-side resilience exists too — rate limiter/bulkhead to protect a service from overload — but breaker/retry/fallback for "my dependency is down" is the caller's job.)
- _2026-06-22 — Caching approach & Redis provider_ — Learn Spring's cache abstraction (`@Cacheable`/`@CacheEvict`) against the **in-memory** cache first, then swap the *provider* to **Upstash Redis** (free, no card, Mumbai region) — proves the code is provider-agnostic (zero change). Chose **cache-aside + evict-on-write** (canonical, simplest-correct) over `@CachePut`; added a **60s TTL** as invalidation insurance and **JSON serialization** so values are human-readable in the console. Set Upstash **Eviction ON** (it's a cache — drop LRU when full, not reject writes). Creds externalized via `${REDIS_*}` (env var in prod / VM args locally) — never committed.
- _2026-06-22 — Kafka infra & cross-service event contract_ — Run Kafka locally in **KRaft mode** (no ZooKeeper, no Docker) — one process, modern default (ZK removed in Kafka 4.0); the company cluster still uses ZooKeeper because it predates the migration. Windows gotchas: run start commands from the Kafka home (or use absolute paths — `bin\windows\*.bat` resolves config *relative to cwd*); repoint `log.dirs` to a real Windows path with **forward slashes** (`\` is an escape in `.properties`). Event = **thin** record (`OrderPlaced`: orderId/customerName/totalAmount/itemCount), past-tense, immutable. Each service keeps its **own copy** of the event class (shared module deferred to Phase 9). **Cross-service deserialization trap:** producer's `JsonSerializer` stamps a `__TypeId__` header naming order-service's package; the consumer must set `spring.json.use.type.headers=false` + `value.default.type` (its own class) or it throws `ClassNotFoundException`. And a **non-web** consumer needs `spring-boot-starter-json` explicitly (spring-kafka treats Jackson as optional; the web starter would otherwise supply it) — else `NoClassDefFoundError: com/fasterxml/jackson/databind/JavaType` at listener start.
- _2026-06-22 — Gateway rate-limiter hang (debugging lesson)_ — The `RequestRateLimiter` route hung on every request while routing itself worked. Two compounding causes: (1) **stale IDE classpath** — after adding a dependency to the pom, STS runs *without* it until **Maven → Update Project (Alt+F5)**, so even `/actuator` 404'd; (2) **wrong `-DREDIS_*` VM args** (a port-typo precedent from catalog). **Fix:** hardcode host+port in gateway props, pass **only** `-DREDIS_PASSWORD`; and verify connectivity via **Actuator** (`/actuator/health` → `redis: UP`) instead of guessing. Lesson: when a reactive call *hangs* (vs erroring), suspect a wrong address, not bad creds (bad creds fail fast); give yourself eyes with a health endpoint + short timeouts before spamming requests.
- _2026-06-22 — Resilience4j gotchas learned_ — (1) `minimumNumberOfCalls` defaults to **100**, so a breaker never opens in a small test until you lower it. (2) `ignore-exceptions` must exclude `FeignException$NotFound` (a 404 is a *correct* answer, not an outage — must not trip the breaker) and retry must also ignore `CallNotPermittedException` (don't retry an already-open breaker). (3) Aspect order is fixed: `Retry(CircuitBreaker(RateLimiter(Bulkhead(call))))`. (4) Bulkhead = concurrency cap; RateLimiter = calls-per-time cap — different failure modes. (5) Config without the matching `@Annotation` is inert.
- _2026-06-23 — Reliable event publish: outbox vs direct send_ — **Transactional Outbox.** Direct `kafkaTemplate.send()` inside `@Transactional create()` is the **dual-write problem**: DB + Kafka are two systems with no shared transaction. Async `send()` (no `.get()`) silently **loses** the event if Kafka is down (order commits anyway); forcing it sync with `.get()` either **fails the order whenever Kafka is down** (availability coupling) or still **races** (Kafka accepted, DB then rolls back → phantom event). You can't atomically commit two systems without 2PC. **Fix = collapse to ONE system:** `create()` writes order + an `outbox` row in one DB tx (truly atomic); a separate `@Scheduled` poller ships rows to Kafka later with retries. Impl rules learned: poller must **wait for the broker ack (`.get()`) BEFORE `markPublished()`** (else it marks "sent" when an async send silently failed); send-then-mark ordering = **at-least-once** (dups possible → idempotency is the next item); the poller and `create()` are **separate transactions on separate threads** — a poller rollback can't undo the already-committed order; and `@Transactional` governs the **database only, never Kafka**.

---

## 8. Open Questions (park them here, we answer together)

- [ ] Multi-module repo vs separate repos for the services? (leaning multi-module)
- [x] Eureka now, or skip straight to K8s DNS for discovery? → **Eureka now** (see Decisions Log 2026-06-22).
- [ ] _add yours..._

---

## 9. Reality check

- This is a **3–6 month** build at one-concept-at-a-time pace. That's fine — the *learning* is the point.
- It's **portfolio-worthy at Phase 3** (🏁). Everything after is added depth, not a gate.
- Next action: **Phase 0** (or jump to **Phase 1** if you want to feel the split first — your call).
