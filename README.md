# Mercato 🛒

> A cloud-native, **event-driven e-commerce platform** built as Spring Boot microservices —
> API gateway, Kafka-based Saga, gRPC, Redis, resilience, full observability, and Kubernetes.

🚧 **Building in public.** A personal learning + portfolio project, built one phase at a time.
See **[docs/ROADMAP.md](docs/ROADMAP.md)** for what's done and what's planned.

---

## Why this project

One e-commerce domain that *naturally* exercises the patterns behind real distributed systems —
service decomposition, synchronous vs. asynchronous communication, distributed transactions (Saga),
resilience, caching, and observability — instead of a pile of disconnected demos. Every technology
is placed where the problem actually calls for it.

## Architecture (target)

```
                          ┌──────────────┐
                          │   Angular     │   storefront + admin (TypeScript)
                          └──────┬───────┘
                                 │  REST + GraphQL  (JWT)
                          ┌──────▼───────┐
                          │ API Gateway  │   Spring Cloud Gateway (routing, edge auth, rate-limit)
                          └──────┬───────┘
       ┌───────────┬────────────┼────────────┬──────────────┐
  ┌────▼────┐ ┌────▼─────┐ ┌────▼──────┐ ┌───▼─────┐ ┌──────▼───────┐
  │  Order  │ │ Catalog  │ │ Inventory │ │ Payment │ │ Notification │
  │ (saga)  │ │ (Redis,  │ │  (gRPC)   │ │         │ │   (Kafka)    │
  │         │ │ GraphQL) │ │           │ │         │ │              │
  └────┬────┘ └────┬─────┘ └────┬──────┘ └────┬────┘ └──────┬───────┘
       └─ each service owns its own database ─┘             │
       │                                                    │
   ════╪════════════ Kafka (event backbone) ════════════════╪════
       OrderPlaced · StockReserved · PaymentCompleted · OrderConfirmed
```

### Communication — chosen per interaction

| Interaction | Style | Mechanism | Why |
|---|---|---|---|
| Order → Catalog | sync | **Feign** | needs the answer now; declarative + load-balanced |
| Order → Inventory | sync | **gRPC** | latency-sensitive hot path; binary + HTTP/2 |
| Order → Payment | sync | **WebClient** | slow external-style call → non-blocking |
| Order → Notification / Analytics | async | **Kafka** | fire-and-forget, eventual consistency, fan-out |

> *Learning note:* this project deliberately uses several communication styles to demonstrate
> each and its trade-offs. A production system would standardize on fewer.

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Cloud Gateway · Spring Data JPA · Flyway · PostgreSQL/H2 ·
Redis · Apache Kafka · gRPC · GraphQL · Resilience4j · Micrometer + Prometheus + Grafana ·
OpenTelemetry · Docker · Kubernetes · Angular

## Services

| Service | Port | Owns | Status |
|---|---|---|---|
| `order-service` | 8080 | orders, saga orchestration | ✅ Phase 0 (hardened: Flyway, Actuator, JWT auth, ProblemDetail) |
| `catalog-service` | 8081 | product catalog | 🚧 skeleton |
| `api-gateway` | — | edge routing & auth | ⏳ planned |
| `inventory-service` | — | stock reservation | ⏳ planned |
| `payment-service` | — | payments | ⏳ planned |
| `notification-service` | — | notifications | ⏳ planned |

## Running locally

Each service is a standalone Spring Boot app (H2 file-mode DB, no external infra needed yet):

```bash
cd order-service   && mvn spring-boot:run    # http://localhost:8080
cd catalog-service && mvn spring-boot:run    # http://localhost:8081
```

Health: `GET /actuator/health` on each. Secrets (e.g. `JWT_SECRET_KEY`) are externalized via
environment variables — the values committed here are local-dev placeholders only.

## Status

Phase 0 (harden the seed) ✅ · Phase 1 (microservices split) 🚧 — full plan in
[docs/ROADMAP.md](docs/ROADMAP.md).

---

*Personal learning / portfolio project. Not affiliated with my employer. Infrastructure runs on
free local or cloud tiers; no real credentials are committed.*
