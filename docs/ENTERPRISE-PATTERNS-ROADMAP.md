# Mercato — Enterprise Patterns & Mastery Roadmap

> **Goal:** turn Mercato into a hands-on tour of the patterns you actually use in large
> enterprise microservice systems — JPA relations, advanced Kafka, concurrency, event-driven
> architecture, and the core design patterns — each built into a *natural* place in the domain
> (never contrived) and to **production best practice**.
>
> **Method:** code-in-chat — I diagnose + give the code piece by piece + explain the *why*,
> **you type it**, I review and run the build/tests. Tests + trivial config are mine.
>
> **Tags:** `[local]` = buildable & testable now on H2, no Docker · `[infra]` = needs the
> container runtime unblock (Oracle VM or WSL2+Docker Engine) to run/verify.
>
> **Companions:** `PRODUCTION-GAPS.md` (the deploy/ops backlog + Phase 0–6), the deep
> concurrency work lives in the separate `kafka_multithreading` "Transaction Risk Engine" project.
>
> **Update (2026-07-19):** the runtime substrate is now **Neon Postgres** (all 5 stateful services) on
> **Spring Boot 3.5.15**. The `[local]` tracks below still build/test on H2 via `@DataJpaTest`, but the
> *running* stack is Postgres. Live hardening plan: [`PRODUCTION-FIX-PLAN.md`](./PRODUCTION-FIX-PLAN.md) · [`TARGET-ARCHITECTURE.md`](./TARGET-ARCHITECTURE.md).

---

## The one principle that shapes everything

**JPA relationships never cross a service boundary.** Each service owns its schema. Map
`@OneToMany`/`@ManyToMany`/`@OneToOne` *inside* one service's aggregate; reference *another*
service's data by **ID + API call**, never a `@ManyToOne` to its table. So we exercise all
relation types by **enriching the services that own the data** (catalog, auth) and adding one
new bounded context — not by wiring entities across services.

---

## Track A — JPA Relations Mastery · `catalog-service` · `[local]`

Covers **every** relation type + value types, the production way. Verified with `@DataJpaTest` on H2.

| Step | Relation | Feature | Best practice taught |
|---|---|---|---|
| A1 | `@ManyToMany` | `Product ↔ Category` | Explicit **`ProductCategory` link entity** (composite `@EmbeddedId` + join attribute `position`) — *not* raw `@ManyToMany`. Why link entities win; bidirectional helpers. |
| A2 | `@OneToOne` | `Product ↔ ProductDetail` | Shared-PK via **`@MapsId`**; the lazy `@OneToOne` proxy trap + fix; 1:1 vs embedding. |
| A3 | `@ElementCollection` | `Product` tags (`Set<String>`) | Value-type collection, `@CollectionTable`; when *not* to make it an entity. |
| A4 | `@Embeddable`/`@Embedded` | `Dimensions` / `Money` value object | Embedded value objects, `@AttributeOverride`. |
| A5 | Cross-cutting | the whole graph | LAZY everywhere + kill **N+1** with `@EntityGraph`/join-fetch; DTO projections (never expose entities); entity `equals/hashCode`; cascade/orphanRemoval scoped to the aggregate; keep `@Version`. |

## Track B — `auth-service` · User ↔ Role as real M2M · `[local]`

Convert the current roles-as-a-`String`-column (R15) into a proper `Role` entity + `user_role`
join. Authorities load from the join. A second M2M in a security context; touches
`UserDetailsService`. Verified with `@DataJpaTest`.

## Track C — New bounded context: `review-service` · `[local]`

A genuinely new service (real capability, not relation filler):
- `Review` aggregate — references `productId`/`userId` **by ID** (the cross-service rule in action).
- `Review 1:N ReviewImage` / `ReviewComment` (`@OneToMany`).
- Rating aggregation read model (feeds Track G's CQRS).
- Own schema (Flyway) + resource-server security + actuator; registers with Eureka; gateway route.
- New Kafka event `ProductReviewed` (fan-out) → analytics.

## Track D — Kafka / Messaging depth

| Step | What | Tag | Note |
|---|---|---|---|
| D1 | **Schema registry** (Avro or Protobuf) + compatibility rules | `[infra]` | The real Kafka gap today (JSON + `default.type` hack has no evolution safety). |
| D2 | **Non-blocking retries** (`@RetryableTopic` / retry+DLT topics) + DLQ reprocessing job | `[infra]` | Replace fixed-backoff blocking retry; drain the DLT. |
| D3 | Manual ack mode + consumer tuning; ordering vs throughput | `[infra]` | Partition-key strategy, `max.poll`, rebalance care. |
| D4 | Exactly-once vs at-least-once — when outbox is enough, when to use Kafka transactions | `[local]` (design) | Mostly a documented decision + diagram. |

## Track E — Event-Driven Architecture / Choreography

Convert **one** saga step (inventory reserve) from synchronous Feign to **choreography**:
`order` emits `StockRequested` → `inventory` consumes, emits `StockReserved`/`StockRejected` →
`order` reacts to finalize. Keeps a concrete **orchestration-vs-choreography** contrast to defend
in interviews. Design `[local]`; **verify `[infra]`** (needs Kafka running).

## Track F — Concurrency & Multithreading · `[local]`

| Step | What | Note |
|---|---|---|
| F1 | Parallelize the saga's per-line-item reserve | `CompletableFuture` over a **bounded** executor + **`DelegatingSecurityContextExecutor`** (propagate the JWT across threads — the ThreadLocal boundary lesson). |
| F2 | `@Async` for genuinely non-critical work (e.g. audit write) | Dedicated `TaskExecutor` bean, sizing, exception handling, context propagation. |
| F3 | Deep concurrency mastery | → the separate **`kafka_multithreading`** project (that's the vehicle; Mercato only has these two honest spots). |

Note: Mercato's *real* concurrency story is **correctness under contention** — pessimistic
`FOR UPDATE`, optimistic `@Version`, idempotency — which is already in place (inventory, orders).

## Track G — Design Patterns Catalog

Reference table: pattern → where it lives in Mercato → the lesson. **✓ = already built**, **+ = to add**.

**Distributed / microservice patterns**
| Pattern | Status | Where |
|---|---|---|
| API Gateway | ✓ | `api-gateway` |
| Service Discovery | ✓ | Eureka `discovery-service` |
| Saga (orchestration) | ✓ | `OrderService.create()` |
| Transactional Outbox | ✓ | `OutboxPoller` |
| Idempotent Consumer | ✓ | notification/analytics Redis dedup |
| Idempotency Key | ✓ | order (R11), inventory reservation (R39) |
| Circuit Breaker / Retry / Bulkhead | ✓ | Resilience4j in `order-service` |
| Rate Limiter | ✓ (bug open) | gateway (Redis) |
| BFF | ✓ | `storefront` (Next.js) in front of the gateway |
| Choreography saga | + | Track E |
| CQRS (read/write split) | + | review ratings read model / catalog search |
| Event Sourcing (lite) | ✓ (lite) | **`OrderStatusHistory`** append-only transition log built (Slice 1) — the "audit via status history" lite version; full event sourcing stays optional |

**Application / GoF patterns**
| Pattern | Status | Where |
|---|---|---|
| Repository | ✓ | every service |
| DTO / Assembler | ✓ | `toDto(...)` projections |
| Adapter / Gateway | ✓ | `CatalogGateway`, Feign clients |
| Proxy (AOP) | ✓ | `@Transactional`, Resilience4j aspects |
| Dependency Injection / Singleton | ✓ | Spring beans |
| Builder | ✓ (partial) | order/entity construction |
| **Specification** | + | dynamic product search (`JpaSpecificationExecutor`) — Track G add |
| **Strategy** | + | pricing/discount or fulfilment strategy — Track G add |
| **Factory / Template Method** | + | where a family of behaviours appears |
| Observer | ✓ (via events) | domain events → Kafka consumers |

---

## Build order (Docker-free first, infra-gated last)

1. **Track A** — JPA relations in catalog *(start here)*
2. **Track B** — auth User↔Role M2M
3. **Track C** — `review-service` (new bounded context; brings CQRS + Specification opportunities)
4. **Track F1/F2** — the two concurrency pieces
5. **Track G adds** — Specification + Strategy + a CQRS read model, woven in where they fit
6. **`[infra]` — once the runtime is unblocked:** Track D (schema registry, retry topics),
   Track E (choreography, verified), then the deploy Phases 1–2 from `PRODUCTION-GAPS.md`.

Each track is independent and committable on its own (personal email, no co-author, conventional commits).
