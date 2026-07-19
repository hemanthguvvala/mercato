# Mercato 🛒

> A cloud-native, **event-driven e-commerce platform** built as Spring Boot microservices —
> service discovery, API gateway, an orchestrated **saga + transactional outbox** over Kafka,
> gRPC, Redis, Resilience4j, and RS256/JWKS security.

🚧 **Building in public.** A personal learning + portfolio project, built one concept at a time.
Phase plan in **[docs/ROADMAP.md](docs/ROADMAP.md)**; the ongoing production-hardening track (post-audit)
is in **[docs/PRODUCTION-FIX-PLAN.md](docs/PRODUCTION-FIX-PLAN.md)** + **[docs/TARGET-ARCHITECTURE.md](docs/TARGET-ARCHITECTURE.md)**.

---

## Why this project

One e-commerce domain that *naturally* exercises the patterns behind real distributed systems —
service decomposition, synchronous vs. asynchronous communication, distributed transactions (Saga),
resilience, caching, and observability — instead of a pile of disconnected demos. Every technology
is placed where the problem actually calls for it.

## Architecture (current)

```
                 ┌───────────────────────────┐
                 │ Next.js storefront (BFF)   │  :3000
                 └─────────────┬─────────────┘
                               │ REST + GraphQL (JWT)
                 ┌─────────────▼─────────────┐
                 │        api-gateway         │  :8090 · edge auth · rate-limit (Redis)
                 └─────────────┬─────────────┘
      ┌──────────┬────────────┼───────────┬───────────┬──────────┐
 ┌────▼───┐ ┌────▼────┐ ┌─────▼────┐ ┌────▼────┐ ┌────▼───┐ ┌────▼───┐
 │ order  │ │ catalog │ │inventory │ │ payment │ │  auth  │ │ review │
 │ :8080  │ │ :8081   │ │ :8082    │ │ :8083   │ │ :8084  │ │ :8087  │
 │ saga·  │ │ GraphQL │ │ gRPC·    │ │ charge/ │ │ RS256/ │ │ reviews│
 │ outbox·│ │ ·Redis  │ │ locks    │ │ refund  │ │ JWKS   │ │        │
 │ FSM    │ │ ·search │ │ :9090    │ │ (sim)   │ │        │ │        │
 └───┬────┘ └─────────┘ └──────────┘ └─────────┘ └────────┘ └───┬────┘
     │ OrderPlaced · OrderConfirmed (via outbox)                │ ProductReviewed
 ════╪══════════════════════ Kafka (KRaft) ═══════════════════════╪════
     └────▶ notification :8085           analytics :8086 ◀────────┘
            (notifications)              (2nd group · fan-out)

 discovery-service :8761 — Eureka registry (every service registers; client-side LB)
 Each service owns its own Neon Postgres database (database-per-service).
```

Cross-service consistency is an **orchestrated saga + a transactional outbox → Kafka**, never a
distributed SQL transaction (no 2PC). No JPA relationship crosses a service boundary — other
services are referenced **by ID**, never a foreign key.

### Communication — chosen per interaction

| Interaction | Style | Mechanism | Why |
|---|---|---|---|
| Order → Catalog | sync | **Feign** | needs the answer now; declarative + load-balanced |
| Order → Inventory | sync | HTTP (locked/idempotent reserve) | latency-sensitive hot path |
| Order → Payment | sync | **WebClient** | slow external-style call → non-blocking client |
| Order → Notification / Analytics | async | **Kafka** | fire-and-forget, eventual consistency, fan-out |

> *Learning note:* this project deliberately uses several communication styles to demonstrate each
> and its trade-offs — a production system would standardize on fewer. The inventory **gRPC** server
> (:9090) runs as a standalone demo; the live saga reserves stock over the locked/idempotent path.

## Tech stack

Java 17 · **Spring Boot 3.5.15** · **Spring Cloud 2025.0.3** (Gateway, OpenFeign, Eureka) ·
Spring Data JPA · **Flyway** · **PostgreSQL — Neon, database-per-service** (+ H2 for tests) ·
Redis (Upstash) · **Apache Kafka (KRaft)** · gRPC · GraphQL · **Resilience4j** (CB / retry / bulkhead / rate-limiter) ·
Spring Security OAuth2 Resource Server (**RS256 / JWKS**) · Micrometer Tracing + Zipkin · Prometheus ·
JUnit 5 · Mockito · Testcontainers · **Next.js** storefront

## Services

A Maven **reactor** of **12 modules** — **10 Spring Boot services + 2 shared libraries** — plus a
separate **Next.js** storefront (its own npm app, not a Maven module).

| Component | Port | Owns / does | Status |
|---|---|---|---|
| `discovery-service` | 8761 | Eureka service registry | ✅ |
| `api-gateway` | 8090 | edge routing, JWT auth, Redis rate-limit | ✅ |
| `auth-service` | 8084 | login → signed JWT (RS256 / JWKS) | ✅ |
| `order-service` | 8080 | orders, **saga orchestrator**, outbox, **state machine** | ✅ |
| `catalog-service` | 8081 | product catalog, Redis cache, **GraphQL**, Specification search | ✅ |
| `inventory-service` | 8082 (gRPC 9090) | stock reservation, optimistic + pessimistic locking | ✅ |
| `payment-service` | 8083 | charge / refund (simulated gateway) on a **durable Postgres ledger** (idempotent) | ✅ |
| `notification-service` | 8085 | Kafka consumer → notifications | ✅ |
| `analytics-service` | 8086 | Kafka consumer (2nd group, **fan-out**) → revenue/count | ✅ |
| `review-service` | 8087 | product reviews (own bounded context) | ✅ |
| `storefront` | 3000 | Next.js storefront (BFF) — *separate npm app, not in the reactor* | 🚧 in progress |
| `events` | — | shared domain-event contracts | ✅ *(library)* |
| `mercato-framework` | — | shared starter (security auto-config, common config) | ✅ *(library)* |

## Running locally

It's a Maven **reactor** (parent `pom.xml`). Infra: a local **Kafka** (KRaft) plus free cloud tiers
for **Neon** Postgres and **Upstash** Redis — no local Docker needed. Each service reads its
DB / Redis / Kafka coordinates and secrets from **environment variables**; nothing sensitive is committed.

```bash
# build everything
mvn -q -DskipTests package

# start discovery first, then the gateway, then services (each in its own shell)
mvn -pl discovery-service spring-boot:run     # :8761
mvn -pl api-gateway       spring-boot:run     # :8090
mvn -pl order-service     spring-boot:run     # :8080
# ...catalog :8081 · inventory :8082 · payment :8083 · auth :8084 · notification :8085 · analytics :8086 · review :8087
```

Common env vars: `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` (Neon, per service), `REDIS_HOST` / `REDIS_PASSWORD`
(Upstash), `SEED_USER_PASSWORD` / `SEED_ADMIN_PASSWORD` (auth seed accounts). Health: `GET /actuator/health` on each.

## Status

- **Phases 0–4 done** — hardened seed · microservices split + gateway + Eureka · caching + Resilience4j ·
  Kafka events + fan-out · **saga + transactional outbox + idempotency + concurrency (no oversell)**.
- **Phases 5–6 in progress** — protocol variety (GraphQL ✅, WebClient ✅, gRPC demo) ·
  observability (metrics + correlated logs ✅, live tracing pending).
- **Production-hardening track** (post-2026-07-18 audit) — **Slice 0 ✅** substrate → Neon Postgres +
  Boot 3.5.15 · **Slice 1 ✅** order state machine + `OrderStatusHistory` · **Slice 2 next** (durable saga reconciler).

Full detail in [docs/ROADMAP.md](docs/ROADMAP.md); the audit and fix backlog in
[docs/PRODUCTION-FIX-PLAN.md](docs/PRODUCTION-FIX-PLAN.md).

---

*Personal learning / portfolio project. Not affiliated with my employer. Payment is a **simulated**
gateway (no real charges). Infrastructure runs on free local or cloud tiers; no real credentials are committed.*
