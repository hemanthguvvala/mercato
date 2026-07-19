# Mercato — Target Architecture (north star)

> **Date:** 2026-07-18 · **Decisions locked:**
> (1) **Deepen the core first**, then widen. (2) **Hybrid saga** — a *durable orchestrator*
> for the command/decision path + *domain-event fan-out* for propagation and read models.
>
> **Method:** this is the blueprint. We implement it as **vertical slices, one concept at a
> time — you code, I review** (not all at once). Fixes from `PRODUCTION-FIX-PLAN.md` get
> *absorbed* into these slices rather than done separately.
>
> **Scope now = the core three:** `order` (orchestrator), `inventory` + `payment` (participants),
> plus an order **read model** (CQRS). New bounded contexts (cart/search/shipping/pricing) are
> designed at the end but **deferred to the widen phase**.
>
> **Progress (2026-07-19):** Slices 0–3 ✅ · **Slice 4 = next.** See §6.

---

## 1. The shape

```
                          ┌─────────────┐
   storefront ──HTTPS──▶  │ api-gateway │  edge auth · rate-limit · TRACE STARTS HERE
                          └──────┬──────┘
              ┌──────────────────┼──────────────────┐
          (commands)          (queries)           (auth)
              │                   │                  │
      ┌───────▼─────────┐   ┌─────▼────────┐   ┌─────▼─────┐
      │  order-service  │   │ order read   │   │ auth-svc  │
      │  ORCHESTRATOR   │   │ model (CQRS) │   └───────────┘
      │  state machine  │   │  order_view  │
      │  saga log       │   └─────▲────────┘
      │  outbox         │         │ projects from events
      └───┬─────────┬───┘         │
   sync   │         │  sync       │
  command │         │  command    │
   ┌──────▼───┐ ┌───▼──────┐      │
   │inventory │ │ payment  │      │
   │ reserve  │ │ ledger   │      │
   │ inbox    │ │ inbox    │      │
   │ outbox   │ │ outbox   │      │
   └────┬─────┘ └────┬─────┘      │
        │ events     │ events     │
        └─────┬──────┴────────────┘
              ▼
       ┌──────────────┐   domain events (via outbox)
       │    Kafka     │──▶ order read-model projector  (order_view)
       │ order.events │──▶ notification
       │ payment.evts │──▶ analytics
       │ stock.evts   │──▶ (widen) shipping-service
       └──────────────┘
```

**Three layers:**
- **Command / write side** — the orchestrator + participants. Owns truth. Short DB transactions, durable state, resilience on every edge.
- **Event backbone** — Kafka. Every writer publishes via **outbox**; every consumer dedups via **inbox**. Real domain events carry state (event-carried state transfer).
- **Read side (CQRS)** — a denormalized `order_view` built by consuming events; all `GET /orders*` reads hit it, never the write DB. Scales independently — this is the "scale the read-heavy path" answer.

---

## 2. How the hybrid saga works (the crux)

**Command path = synchronous orchestration with durable state** (recommended). The storefront's
"place order" waits for a real result, so the *decision* steps stay synchronous — but every step is
**persisted before the call and reconciled after**, so a crash is recoverable:

```
place order ─▶ order-service (orchestrator)
  1. persist Order = CREATED                    (commit)
  2. transition → RESERVING; call inventory.reserve (sync, CB+timeout)
        success → persist STOCK_RESERVED         (commit)
        reject  → persist FAILED; compensate; return 409
  3. transition → AUTHORIZING; call payment.authorize (sync, CB+timeout)
        success → persist PAYMENT_AUTHORIZED      (commit)
        decline → persist COMPENSATING; release stock; FAILED; return 402
  4. transition → CONFIRMED  (commit) + write OrderConfirmed to OUTBOX (same tx)
  5. return 201
```

**Propagation = event-driven.** After CONFIRMED, the outbox publishes `OrderConfirmed`; the read-model
projector, notification, analytics (and later shipping) all react. **The command path is orchestrated &
safe; everything downstream is genuinely event-driven.**

**Recovery (what makes it durable):** a `@Scheduled` **reconciler** scans orders stuck in a non-terminal
state (`RESERVING`/`AUTHORIZING`/`COMPENSATING`) older than N seconds and drives them forward or
compensates — idempotently, off the persisted `saga_step` log. A JVM crash after the charge no longer
loses money: the reconciler sees `PAYMENT_AUTHORIZED` with no `CONFIRMED` and finishes (or refunds).

> **Sub-decision (my recommendation, flag if you disagree):** keep reserve+authorize **synchronous**
> for the user-facing command (simplest correct UX, keeps your parallel-reserve + resilience4j work).
> The fully-async variant — orchestrator sends command *messages* and reacts to reply events — is a
> later evolution, not now; it complicates the user-facing latency/timeout story for little extra learning
> at this stage.

---

## 3. Entity / schema changes (the "restructure" you green-lit)

Per-service schema ownership stays sacred: **no JPA relationship crosses a service boundary** — other
services are referenced by **ID**, never a foreign key.

### order-service (orchestrator) — Postgres `orderdb`
- **`Order`** (aggregate root): ✅ **built (Slice 1)** — `status` enum `{PENDING, RESERVING, STOCK_RESERVED, AUTHORIZING, PAYMENT_AUTHORIZED, CONFIRMED, COMPENSATING, FAILED, CANCELLED}` with a `canTransitionTo` guard + `transitionTo()`; keep `@Version`, `idempotencyKey`, `customerId` (by ID). *(Start state is `PENDING`, not `CREATED` — matches the existing saga's PENDING-first commit.)*
- **`OrderItem`** (existing `@OneToMany`).
- **`OrderStatusHistory`** (new, append-only): ✅ **built (Slice 1)** — `orderId, fromStatus, toStatus, changedAt` (add a `reason` column when the reconciler needs it) — audit + debugging of the state machine.
- **`SagaStep`** — *considered, not built (Slice 2 decision).* We reconcile off the order **`status` + an `updatedAt` heartbeat** instead: the steps are coarse and downstream ops are idempotent, so status-level recovery is enough, and `OrderStatusHistory` already records every step. Revisit a per-step `SagaStep` log only if finer-grained partial-step recovery is ever needed.
- **`OutboxEvent`** (improve): index on `published`, retention/purge job, `SKIP LOCKED` claim, optional `traceparent` column (for F15).
- **`IdempotencyRecord`** (fix): store the **terminal outcome** (success id or failure) so a failed order doesn't poison the key.

### inventory-service (participant) — Postgres `inventorydb`
- **`InventoryItem`** (`@Version`, existing).
- **`StockReservation`** (existing): add `status {RESERVED, RELEASED, CONFIRMED}`, keep unique `(orderId, productId)` → idempotent reserve/release.
- **`InboxMessage`** (new): dedup consumed commands/events by messageId.
- **`OutboxEvent`** (new): emits `StockReserved` / `StockRejected` / `StockReleased`.

### payment-service (participant) — Postgres `paymentdb` (was in-memory!)
- **`Payment`** ✅ **built (Slice 3)** — `id, orderId (unique), amount, status {AUTHORIZED, REFUNDED}, createdAt` — durable ledger, not a `ConcurrentHashMap`. (`orderId` is the idempotency key; status set kept minimal — `CAPTURED`/`DECLINED` deferred until a step needs them.)
- **`InboxMessage`** + **`OutboxEvent`**: emits `PaymentAuthorized` / `PaymentDeclined` / `PaymentRefunded`.

### order read model — Postgres `orderquerydb` (own schema; own service in the widen phase)
- **`OrderView`** (denormalized): `orderId, customerId, status, total, itemsJson, createdAt, updatedAt` — built by consuming order events; serves `GET /orders`, history, status. Fixes the unbounded `findAllWithItems()` and the object-level-authz leak (query by `customerId`).

---

## 4. The `events` module — real domain events (versioned)

One canonical, shared contract per event (you already proved the shared-module pattern with `OrderPlaced`):

| Event | Emitted by | Consumed by | Carries |
|---|---|---|---|
| `OrderCreated` | order | read-model | orderId, customerId, items, total |
| `StockReserved` / `StockRejected` | inventory | order, read-model | orderId, productId(s), reason |
| `PaymentAuthorized` / `PaymentDeclined` | payment | order, read-model | orderId, amount, reason |
| `OrderConfirmed` | order | read-model, notification, analytics, (shipping) | orderId, customerId, total |
| `OrderCancelled` / `OrderRefunded` | order | read-model, notification, analytics | orderId, reason |

`OrderPlaced` is subsumed by `OrderConfirmed`. Schema-registry + Avro/Protobuf compatibility rules are
the **widen-phase** hardening (F22); for now, versioned records + the inbox dedup carry us.

---

## 5. How this absorbs the fix plan

These `PRODUCTION-FIX-PLAN.md` items are no longer standalone patches — they're **built correctly the first time** inside the slices below:

| Fix | Becomes |
|---|---|
| F4 saga durability, F5 shutdown | Slice 2 (state machine + saga log + reconciler) |
| F9 payment idempotency/OOM | Slice 3 (payment ledger entity) |
| F10 outbox safety, F21 dedup, F38 events | Slice 4 (outbox/inbox + real events) |
| F17 pagination, GET /orders authz | Slice 5 (CQRS read model) |
| F6 breaker on money path, F18/F19 gateway | Slice 6 |
| F15/F16/F24/F25 observability | Slice 7 |
| F3 Postgres, F1/F2 security quick wins | Slice 0 (precondition) |

Still done as **standalone** fixes (not part of the redesign): F7 authz, F11 rate-limit key, F23 Kafka TLS, F26 profiles, F31 deps — fold them in as we touch each area.

---

## 6. Build sequence — vertical slices (deepen-first)

Each slice is buildable, reviewable, and teaches **one core concept**. We do them in order.

- **Slice 0 — Substrate & quick wins.** ✅ **DONE (2026-07-19).** F1 (H2 console off), F2 (default creds), F3 (all **5 stateful services** → **Neon Postgres**, database-per-service; Flyway kept via `flyway-database-postgresql`). Bonus: Boot 3.5.15 / Cloud 2025.0.3 upgrade (F31). *Concept: shared DB as the precondition for everything below.* *(payment is still in-memory → its durable ledger is Slice 3, not Slice 0.)*
- **Slice 1 — Order state machine.** ✅ **DONE (2026-07-19).** `OrderStatus` enum + `canTransitionTo` guard + `OrderEntity.transitionTo()` writing append-only `OrderStatusHistory` (V6 migration); saga now walks PENDING→RESERVING→STOCK_RESERVED→AUTHORIZING→PAYMENT_AUTHORIZED→CONFIRMED, and on failure →COMPENSATING→FAILED. 9 unit tests green. *Concept: aggregate + explicit state machine (no more implicit status).*
- **Slice 2 — Durable saga + reconciler.** ✅ **DONE (2026-07-19).** `OrderReconciler` (`@Scheduled`) recovers stuck orders off **order status + an `updatedAt` heartbeat** (chosen over a separate `SagaStep` table). Point-of-no-return split: pre-payment → compensate (backward), `PAYMENT_AUTHORIZED` → `confirm()` (forward); `confirm()` guarded inside `create()`; `server.shutdown=graceful` added (F4/F5/F12/F13). **+ ★ inventory reservation expiry** — `ReservationExpirySweep` releases abandoned `RESERVED` holds after a TTL (paid ones excluded once Slice 4 marks them `CONFIRMED`). 8 unit tests green; full order+inventory suites green on Boot 3.5.15. *Concept: durable saga / process manager + reconciliation.* **The crown-jewel interview story.**
- **Slice 3 — Payment as a durable ledger.** ✅ **DONE (2026-07-19).** `Payment` entity + `PaymentRepository`; `charge`/`refund` `@Transactional` on the ledger with `unique(order_id)` as the exactly-once guard; in-memory maps deleted (F9). 5 unit tests green. *Concept: idempotent ledger, exactly-once effect.*
- **Slice 4 — Real events + outbox/inbox.** Expand `events`; outbox on payment+inventory; inbox dedup on every consumer. *Concept: event-carried state transfer, inbox/outbox, at-least-once made safe.* **← EDA becomes real here.**
- **Slice 5 — CQRS read model.** `OrderView` projector from events; `GET /orders*` reads it; per-customer authz + pagination. *Concept: CQRS read/write split.*
- **Slice 6 — Resilience on the money path.** CB+TimeLimiter on payment+inventory; gateway timeout+CB; rate-limit key fix. *Concept: bulkhead the critical path; stop the cascade.*
- **Slice 7 — Observability end-to-end.** Tracing edge→Kafka→consumer; Prometheus everywhere; business metrics; structured logs; scrub PII. *Concept: trace one order across every hop.*

**Then widen** (new services, their own slices): `search-service` (catalog CQRS projection) → `cart-service` → `shipping-service` (a 4th saga participant, reacts to `OrderConfirmed`) → `pricing/promotion-service` (Strategy). Split the order read model into its own `order-query-service`. Plus Kafka hardening (schema registry, TLS, DLT drain).

---

## 7. Interview framing (why these choices)

- **"Is it event-driven?"** → *Hybrid.* Orchestrated command path (a durable state machine — traceable, one place owns the decision) + event-driven propagation (read models, notifications, downstream services react). "We chose orchestration for the money-critical decision path because it's easier to reason about and recover, and events for everything downstream because it decouples and scales. Pure choreography spreads the flow across services and leans entirely on tracing — a real trade-off, not a default."
- **"How do you not lose money?"** → durable saga state + reconciler + idempotent, compensatable steps; the outbox guarantees the event iff the order committed.
- **"How do you scale reads?"** → CQRS: a read model updated by events, scaled independently of the write side.
- **"Orchestration vs choreography?"** → you can now defend *both* because you built the orchestrated core and can point at where choreography (shipping reacting to `OrderConfirmed`) fits.

---

*This supersedes the "forward roadmap" section of the old review and elevates `ENTERPRISE-PATTERNS-ROADMAP.md`
Tracks E (choreography) + G (CQRS) into the actual target. Widen-phase service designs live there.*
