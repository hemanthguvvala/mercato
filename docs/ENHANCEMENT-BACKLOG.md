# Mercato — Master Enhancement Backlog

> **Purpose:** one place that captures **every** enhancement idea for Mercato — from our own
> deep audit (`PRODUCTION-FIX-PLAN.md`), the forward design (`TARGET-ARCHITECTURE.md`,
> `ENTERPRISE-PATTERNS-ROADMAP.md`, `PRODUCTION-GAPS.md`), **and two external portfolio reviews**
> (2026-07-18). **Nothing is excluded** — items we're deferring are kept here *with the reason*,
> because deciding what *not* to build (and why) is itself an interview-grade signal.
>
> This doc does not replace the others — it's the **superset index**. Each row points to where it's
> tracked/implemented.
>
> **Status:** ✅ done · 📋 planned (with pointer) · 🆕 new (from the reviews, not yet in any doc)
> **Priority:** ⭐⭐⭐⭐⭐ highest interview/production ROI → ⭐⭐☆☆☆ nice-to-have

---

## A. Deployment & infrastructure
| Item | Why | Status | Pri |
|---|---|---|---|
| **Docker Compose** — one command boots Postgres+Kafka+Redis+Zipkin/OTel+Eureka+gateway+all services | makes the whole thing runnable/demoable in one shot | 📋 `PRODUCTION-GAPS` Phase 1 (blocked only by the no-Docker work machine; Neon+Upstash already cover data) | ⭐⭐⭐⭐⭐ |
| **Kubernetes** — Deployment/Service/ConfigMap/Secret/Ingress/HPA | answers "how do you deploy microservices?" + enables real multi-replica (now that DBs are shared) | 📋 `PRODUCTION-GAPS` Phase 2 | ⭐⭐⭐⭐⭐ |
| **Helm charts** | templated, environment-parameterized deploys | 🆕 (pairs with K8s) | ⭐⭐⭐⭐☆ |
| **Blue/green · canary · automated rollback** | safe releases | 📋 `PRODUCTION-GAPS` §E | ⭐⭐⭐☆☆ |
| **Multi-AZ / multi-region, DR (RTO/RPO)** | resilience | 📋 `PRODUCTION-GAPS` §D/F | ⭐⭐☆☆☆ |

## B. Observability
| Item | Why | Status | Pri |
|---|---|---|---|
| **OpenTelemetry SDK + OTLP exporter → Jaeger / Grafana Tempo** (over Brave/Zipkin) | industry is migrating to OTel; vendor-neutral | 📋 Slice 7 (upgrade the F15 approach to OTel) | ⭐⭐⭐⭐⭐ |
| **Trace survives HTTP → Kafka → consumer**, all 10 services | today it dies at the Kafka boundary; can't follow an order end-to-end | 📋 Slice 7 / F15 | ⭐⭐⭐⭐⭐ |
| **Prometheus on every service + custom business metrics** | alert on payment-failure rate, reservation conflicts, saga compensations | 📋 Slice 7 / F16 | ⭐⭐⭐⭐⭐ |
| **Centralized logging — Loki+Grafana or ELK** | one place to search all services' logs | 📋 `PRODUCTION-GAPS` §C | ⭐⭐⭐⭐☆ |
| **Structured JSON logs (`logback-spring.xml`) + correlation IDs + PII scrub** | machine-parseable, greppable by trace id | 📋 F24 / F25 | ⭐⭐⭐⭐☆ |
| **Grafana dashboards + SLO-based alerting + error budgets** | Day-2 ops | 📋 `PRODUCTION-GAPS` §C/I | ⭐⭐⭐☆☆ |

## C. Configuration & secrets
| Item | Why | Status | Pri |
|---|---|---|---|
| **Spring Cloud Config Server** (or Consul) | centralized, versioned config | 🆕 | ⭐⭐⭐⭐☆ |
| **HashiCorp Vault / KMS** for secrets | no secrets in env/source | 📋 `PRODUCTION-GAPS` §A (+ our F2/F4) | ⭐⭐⭐⭐☆ |
| **Dev/prod Spring profiles + safe-by-default** | one artifact, many environments | 📋 F26 | ⭐⭐⭐⭐☆ |

## D. Events, CQRS & messaging
| Item | Why | Status | Pri |
|---|---|---|---|
| **CQRS read model — actually implement** (write DB → Kafka → read model → dashboard) | scale reads independently; classic interview topic | 📋 TARGET-ARCH **Slice 5** + ENTERPRISE Track G | ⭐⭐⭐⭐⭐ |
| **Inbox pattern** (dedup table → exactly-once *processing*) | complements outbox + idempotent consumer | 📋 TARGET-ARCH **Slice 4** | ⭐⭐⭐⭐☆ |
| **Event versioning** (`OrderCreated` v1→v2, backward-compatible consumers) | most projects ignore this; strong senior discussion | 🆕 (pairs with schema registry) | ⭐⭐⭐⭐☆ |
| **Schema registry (Avro/Protobuf) + compatibility rules** | the real Kafka evolution gap today | 📋 ENTERPRISE Track D | ⭐⭐⭐⭐☆ |
| **Choreography saga** (one step reserve→events) | orchestration-vs-choreography contrast | 📋 ENTERPRISE Track E | ⭐⭐⭐⭐☆ |
| **Non-blocking retries / retry topics (`@RetryableTopic`) + DLQ drain/reprocess job** | replace blocking fixed-backoff; close the DLT loop | 📋 ENTERPRISE Track D / F-DLQ | ⭐⭐⭐☆☆ |
| **`OrderFailed`/`OrderCancelled` events** | only happy-path event exists today | 📋 F38 | ⭐⭐⭐☆☆ |

## E. Domain features (real e-commerce behavior)
| Item | Why | Status | Pri |
|---|---|---|---|
| **★ Inventory reservation expiry** — `@Scheduled` job releases stock for unpaid orders after N min → emits `StockReleased` | mirrors real e-commerce; **reuses Slice 2's reconciler machinery almost for free** | 🆕 → **fold into Slice 2** | ⭐⭐⭐⭐⭐ |
| **Async payment / webhook flow** — `PaymentPending → gateway → webhook → Kafka → OrderConfirmed` | interviewers love webhook flows; the async-saga story | 🆕 → new track (pairs with ENTERPRISE Track E) | ⭐⭐⭐⭐⭐ |
| **Order status endpoint + state machine** | falls out of the state machine | ✅ Slice 1 (state machine done; status endpoint next) | ⭐⭐⭐⭐☆ |
| **Product search/pagination** in catalog | real e-commerce need | 📋 `PRODUCTION-GAPS` / Specification done | ⭐⭐⭐☆☆ |

## F. Security
| Item | Why | Status | Pri |
|---|---|---|---|
| **Money/stock endpoint authorization** (`@PreAuthorize`) | any user can charge/refund today | 📋 F7 / F14 | ⭐⭐⭐⭐⭐ |
| **Refresh tokens + rotation** | short-lived access tokens, seamless renewal | 📋 `PRODUCTION-GAPS` §A | ⭐⭐⭐⭐☆ |
| **Token revocation / blocklist** | stolen JWT valid until expiry today | 📋 `PRODUCTION-GAPS` §A | ⭐⭐⭐⭐☆ |
| **JWKS key rotation (`kid`) + persisted keypair** | one ephemeral key today → breaks HA | 📋 F8 | ⭐⭐⭐⭐☆ |
| **mTLS between services (or service mesh)** | zero-trust internal traffic | 📋 `PRODUCTION-GAPS` §A / R24 | ⭐⭐⭐☆☆ |
| **Fine-grained roles/permissions** | beyond USER/ADMIN | 📋 F14 | ⭐⭐⭐☆☆ |
| **Comprehensive audit logging** | who did what | 📋 `PRODUCTION-GAPS` §A (audit controller exists) | ⭐⭐⭐☆☆ |
| **Security headers (HSTS/CSP), CORS/CSRF** | with the frontend | 📋 `PRODUCTION-GAPS` / F41 | ⭐⭐⭐☆☆ |
| H2 console off · seed creds from env · iss/aud validation | | ✅ F1 / F2 / R36 | — |

## G. Performance
| Item | Why | Status | Pri |
|---|---|---|---|
| **Circuit breaker + TimeLimiter on payment/inventory** | only catalog protected today → cascade risk | 📋 F6 | ⭐⭐⭐⭐⭐ |
| **Redis cache-aside + stampede prevention + TTL + write-through** | proper caching on hot reads | 📋 F32 | ⭐⭐⭐⭐☆ |
| **Pagination on all list endpoints** | unbounded queries today | 📋 F17 | ⭐⭐⭐⭐☆ |
| **HikariCP tuning + PgBouncer** | pool sizing × replicas vs Postgres max_connections | 📋 F3 note / `PRODUCTION-GAPS` §B | ⭐⭐⭐⭐☆ |
| **Async writes** (e.g. audit, non-critical) | keep the request path fast | ✅/📋 partial (`@Async` audit exists) | ⭐⭐⭐☆☆ |
| **GZIP compression** | smaller payloads | 🆕 | ⭐⭐⭐☆☆ |
| **ETag / HTTP caching** | conditional GETs on catalog | 🆕 | ⭐⭐⭐☆☆ |
| **Request batching** | fewer round-trips | 🆕 | ⭐⭐☆☆☆ |

## H. Data layer
| Item | Why | Status | Pri |
|---|---|---|---|
| **Read replicas** | scale reads | 📋 `PRODUCTION-GAPS` §B | ⭐⭐⭐⭐☆ |
| **Indexing strategy + query-plan review + N+1 audit** | performance | 📋 `PRODUCTION-GAPS` §B (F10 index-on-published) | ⭐⭐⭐⭐☆ |
| **Data retention / archival** (outbox, audit, history grow forever) | tables grow unbounded | 📋 `PRODUCTION-GAPS` §B / F10 | ⭐⭐⭐☆☆ |
| **PostgreSQL partitioning** | large tables (orders, history) | 🆕 | ⭐⭐⭐☆☆ |
| **Materialized views** | precomputed read models (feeds CQRS) | 🆕 (pairs with Slice 5) | ⭐⭐⭐☆☆ |
| **JSONB usage** | flexible attributes where a document shape helps | 🆕 | ⭐⭐☆☆☆ |
| **Soft delete** | recoverable deletes / audit | 🆕 | ⭐⭐☆☆☆ |
| Backups + PITR, encryption at rest | | 📋 `PRODUCTION-GAPS` §B | ⭐⭐⭐☆☆ |

## I. API & contract management
| Item | Why | Status | Pri |
|---|---|---|---|
| **API versioning** (`/api/v1`, `/api/v2`) + deprecation policy | evolve without breaking clients | 📋 `PRODUCTION-GAPS` §K | ⭐⭐⭐⭐☆ |
| OpenAPI/Swagger docs | | ✅ `openapi-starter` | — |
| **Distributed locking** (Redis lock / Postgres advisory lock) for flash sales | hot-SKU contention beyond row locks | 🆕 | ⭐⭐⭐⭐☆ |

## J. Multi-tenancy
| Item | Why | Status | Pri |
|---|---|---|---|
| **Tenant isolation** — Tenant ID + Hibernate filter, schema/db-per-tenant | SaaS-shaped systems | 🆕 | ⭐⭐⭐☆☆ |

## K. Testing
| Item | Why | Status | Pri |
|---|---|---|---|
| **Testcontainers integration** (Postgres/Kafka/Redis) | prove it on real infra | ✅/📋 partial (analytics + catalog done; order outbox next) | ⭐⭐⭐⭐☆ |
| **Consumer-driven contract tests** (Pact / Spring Cloud Contract) | catch the DTO-drift risk | 📋 `PRODUCTION-GAPS` §H | ⭐⭐⭐⭐☆ |
| **Load / stress / soak testing** (k6 / Gatling) | nothing proven under load | 📋 `PRODUCTION-GAPS` §G | ⭐⭐⭐⭐☆ |
| **Chaos / fault-injection** (Toxiproxy / Chaos Monkey) | resilience under failure | 📋 `PRODUCTION-GAPS` §D | ⭐⭐⭐☆☆ |
| **Coverage + mutation gates** | quality bar | 📋 `PRODUCTION-GAPS` §H | ⭐⭐⭐☆☆ |

## L. DevOps & code quality
| Item | Why | Status | Pri |
|---|---|---|---|
| **Dependabot + OWASP dependency-check** | CVE detection (still Boot-stale history) | 📋 F31 / R25 (Dependabot pending) | ⭐⭐⭐⭐☆ |
| **Trivy image scanning + non-root/distroless images** | supply-chain security | 📋 `PRODUCTION-GAPS` §A | ⭐⭐⭐⭐☆ |
| **SonarQube / SpotBugs (static analysis)** | code quality gate | 🆕 / `PRODUCTION-GAPS` §H | ⭐⭐⭐☆☆ |
| **JaCoCo coverage** | coverage reporting | 🆕 | ⭐⭐⭐☆☆ |
| **Fix the 2 Boot-3.5 deprecations** (`@MockBean`→`@MockitoBean`, `Specification.where`) + run full suite | clean upgrade | 📋 (this session's follow-up) | ⭐⭐⭐☆☆ |
| Semantic versioning | | 🆕 | ⭐⭐☆☆☆ |
| Conventional commits + reactor POM/BOM | | ✅ (in use; R30/R31 done) | — |
| CI: GitHub Actions build+test | | ✅ (R25 pipeline exists) | — |

## M. Documentation
| Item | Why | Status | Pri |
|---|---|---|---|
| **C4 diagrams** (context/container/component) | the architecture money-shot | 🆕 / `PRODUCTION-GAPS` §L | ⭐⭐⭐⭐☆ |
| **Sequence diagrams for the saga flow** | shows the order lifecycle | 🆕 | ⭐⭐⭐⭐☆ |
| **ER diagrams per service** | data model clarity | 🆕 | ⭐⭐⭐☆☆ |
| **ADRs (Architecture Decision Records)** | why each choice + trade-offs | 📋 `PRODUCTION-GAPS` §L | ⭐⭐⭐⭐☆ |
| Top-level README (architecture shot + honesty note) | first impression | 📋 `PRODUCTION-GAPS` Phase 9 | ⭐⭐⭐⭐☆ |

## N. gRPC decision
| Item | Why | Status | Pri |
|---|---|---|---|
| **Resolve the dead gRPC surface** — either wire inventory service-to-service over gRPC (a real path) *or* remove it | today it's unused **and unauthenticated** (F20); an idle surface invites 20 min of interview questions on something not integrated | 📋 F20 / F33 (decision pending) | ⭐⭐⭐⭐☆ |

---

## O. Considered and deliberately deferred (kept, not excluded — with reasons)
> Documenting what you *chose not to add* is a senior signal. Revisit if a real use case appears.

| Tech | Why deferred |
|---|---|
| **RabbitMQ** | Kafka already covers messaging; adding a 2nd broker is complexity without a use case |
| **MongoDB** | no service genuinely needs a document model today (revisit for a catalog/CMS-shaped context) |
| **Cassandra** | no wide-column/write-scale need at this size |
| **Elasticsearch** | only if we build advanced product search (typo tolerance, autocomplete) — natural fit *if* it feeds from the CQRS read model (Slice 5) |
| **Object storage (S3/MinIO)** | for product/review images — nice-to-have; slots in with the review-images feature |
| **GraphQL everywhere** | keep it catalog-only; per-service GraphQL is over-engineering |

---

## P. Consolidated roadmap (merges both reviews + our slice plan)

**Now — the deepen-core slices (in progress):**
- ✅ Slice 0 (Postgres db-per-service, Boot 3.5.15 upgrade, security quick wins) · ✅ Slice 1 (order state machine)
- **Slice 2 — durable saga reconciler + ★ inventory reservation expiry** (crown jewel; the reconciler powers both)
- Slice 3 payment durable ledger · Slice 4 real events + inbox/outbox · Slice 5 CQRS read model · Slice 6 resilience on money path · Slice 7 observability (OTel, trace across Kafka, Prometheus everywhere)

**Highest-ROI infra (from both reviews):**
1. **Docker Compose** (one-command stack) → **Kubernetes + Helm** → OpenTelemetry + centralized logging
2. Distributed config/secrets (Config Server / Vault) · Feature flags (OpenFeature/Unleash)

**Depth for senior signal:**
- CQRS (implement) · API versioning · Refresh tokens + revocation + key rotation · Comprehensive audit logs · Event versioning · Inbox pattern · Async payment/webhook · Distributed locking (flash sales)

**Prove it:**
- Load testing (k6/Gatling) · Contract tests (Pact) · Chaos (Toxiproxy) · SonarQube/JaCoCo/Trivy/Dependabot

**Explain it:**
- C4 + sequence + ER diagrams · ADRs · README money-shot

**Later / if-use-case:** Multi-tenancy · Elasticsearch (via CQRS) · Object storage · PG partitioning · Materialized views · Read replicas · Blue-green/canary · Multi-region DR

---

*Sources folded in here: our `PRODUCTION-FIX-PLAN.md` (F1–F39), `TARGET-ARCHITECTURE.md` (Slices 0–7),
`ENTERPRISE-PATTERNS-ROADMAP.md` (Tracks A–G), `PRODUCTION-GAPS.md` (domains A–L), plus two external
portfolio reviews (2026-07-18). Nothing dropped — deferred items live in §O with their rationale.*
