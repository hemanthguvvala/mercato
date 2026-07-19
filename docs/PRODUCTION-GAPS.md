# Mercato — Production Readiness Gaps & Deployment Backlog

> Companion to `PRODUCTION-READINESS-REVIEW.md` (the R1–R42 findings).
> That doc = code-level findings. **This doc = the full production surface area**:
> everything between "works on my machine" and "runs for real at scale."
>
> Status legend: ✓ done · ⚠ partial · ✗ missing
> Effort tag: **[local]** closeable & verifiable on this machine ·
> **[infra]** needs a real cloud/datastore/cluster to do or prove ·
> **[ops]** a process/discipline, not just code
>
> **Update (2026-07-19):** the substrate migration has begun — all 5 stateful services are now on
> **Neon Postgres** (Slice 0 / F3) and the stack is on **Spring Boot 3.5.15 / Cloud 2025.0.3**. The
> DB-tier rows below are refreshed; the infra/ops/deployment rows still stand as written. Live,
> code-accurate plan: [`PRODUCTION-FIX-PLAN.md`](./PRODUCTION-FIX-PLAN.md).

## How to read this (the 3 tiers)

1. **Patterns / correctness — STRONG.** Auth (RS256/JWKS), resilience (CB/retry/timeout/bulkhead),
   idempotency, transactional outbox, saga. The hard, differentiating stuff, done right.
2. **Substrate — learning-tier BY CHOICE.** H2, single Redis, 3 Kafka partitions, local-only.
   Swappable; not conceptually hard; needs infra to close.
3. **Operations / Day-2 — NEAR ZERO.** Observability, CI/CD, testing depth, runbooks, SLOs, DR.
   Invisible in a demo, decisive in production. This is the real gap.

Honest positioning: **production-*shaped*, not production-*proven*.** The design decisions are the
ones you'd make for scale; the substrate and operations are not yet there — and naming that gap
precisely is stronger in an interview than claiming "production-ready."

---

## A. Security
| Factor | Status | Effort | Note / R-link |
|---|---|---|---|
| AuthN (RS256/JWKS, hand-rolled) | ✓ | — | The standout. |
| AuthZ (roles, `@PreAuthorize`) | ✓ | — | R14. |
| Validate `iss` / `aud` | ⚠ | [local] | R36 — currently sig+expiry only. |
| Token revocation / refresh flow | ✗ | [local] | No refresh tokens, no blocklist; stolen JWT valid until expiry. |
| JWKS key rotation strategy | ✗ | [local] | One key forever; prod rotates `kid`. |
| Secrets management (Vault/KMS) | ✗ | [infra] | Passwords in env vars. |
| TLS in transit (svc↔svc, Kafka SASL/TLS) | ⚠ | [infra] | R24 partial; internal traffic plaintext. |
| Encryption at rest | ✗ | [infra] | |
| Dependency / CVE scanning | ✗ | [local] | R25. |
| Container image scan + non-root + distroless | ✗ | [local] | |
| Bean Validation on every DTO | ⚠ | [local] | Spotty. |
| CORS / CSRF | ✗ | [local] | R41 — do with frontend. |
| Security headers (HSTS/CSP) | ✗ | [local] | |
| WAF / DDoS | ✗ | [infra] | |
| PII redaction in logs | ✗ | [local] | |
| Audit logging | ⚠ | [local] | Audit controller exists; not comprehensive. |

## B. Data & persistence
| Factor | Status | Effort | Note |
|---|---|---|---|
| Real clustered DB (Postgres) | ✓ | [infra] | **Done 2026-07-19** — all 5 stateful services on **Neon Postgres**, database-per-service (Slice 0 / F3). The H2 single-replica substrate blocker is removed. |
| Migrations (Flyway) | ✓ | — | V1–V6 (V6 = `order_status_history`); `flyway-database-postgresql` module now on the classpath so Flyway can speak Postgres. |
| Backups + PITR | ✗ | [infra] | |
| Read replicas / conn proxy (PgBouncer) | ✗ | [infra] | |
| Data retention / archival | ✗ | [local] | Outbox & audit tables grow forever. |
| Index / query-plan review, N+1 audit | ✗ | [local] | |
| Pagination on all list endpoints | ⚠ | [local] | |

## C. Observability (biggest operational gap)
| Factor | Status | Effort | Note |
|---|---|---|---|
| Health / readiness / liveness | ✓ | — | R33. |
| Metrics → Prometheus | ⚠ | [local] | Actuator present; no export pipeline. |
| Distributed tracing (end-to-end) | ✗ | [infra] | No correlation ID across 5 hops. |
| Centralized structured logs (Loki/ELK) | ✗ | [infra] | stdout only. |
| Dashboards (Grafana) | ✗ | [infra] | |
| SLO-based alerting | ✗ | [ops] | |
| Business metrics (alertable) | ✗ | [local] | |

## D. Reliability & resilience
| Factor | Status | Effort | Note |
|---|---|---|---|
| CB / retry / timeout / bulkhead | ✓ | — | Correctly ordered. |
| Idempotency (API + consumer) | ✓ | — | R6/R11. |
| Transactional outbox | ✓ | — | R5. |
| Multi-replica outbox safety (SKIP LOCKED) | ✗ | [infra] | R7 — needs Postgres + replicas. |
| DLQ | ⚠ | [local] | Exists; no drain / reprocess / alert loop. |
| Graceful shutdown | ✓ | — | R33. |
| Backpressure / load shedding | ⚠ | [infra] | Sheds, but not tuned to real capacity. |
| Chaos / fault-injection testing | ✗ | [infra] | |
| Multi-AZ / multi-region failover | ✗ | [infra] | |
| RTO / RPO defined | ✗ | [ops] | |

## E. Deployment & release
| Factor | Status | Effort | Note |
|---|---|---|---|
| Dockerfiles (per service) | ✗ | [local] | **First step.** |
| docker-compose full-stack (local) | ✗ | [local] | **The unlock milestone.** |
| CI/CD pipeline (build/test/gate) | ✗ | [local] | |
| K8s manifests / Helm | ✗ | [infra] | |
| Rolling / canary / blue-green | ✗ | [infra] | |
| Automated rollback | ✗ | [infra] | |
| Controlled DB migration in pipeline | ✗ | [ops] | Flyway-on-startup risky at N replicas. |
| Feature flags | ✗ | [local] | |
| Dev / staging / prod parity | ✗ | [infra] | |

## F. Infrastructure & platform
| Factor | Status | Effort | Note |
|---|---|---|---|
| Orchestration (k8s / k3s) | ✗ | [infra] | |
| Autoscaling (HPA / cluster) | ✗ | [infra] | |
| Ingress + gateway + LB + DNS + TLS term | ⚠ | [infra] | Gateway ✓; rest missing. |
| Config externalization | ✓ | — | R18. |
| Certificate management | ✗ | [infra] | |
| Service discovery | ⚠ | [infra] | Hardcoded/Feign; no registry. |

## G. Performance & capacity
| Factor | Status | Effort | Note |
|---|---|---|---|
| Load / stress / soak testing | ✗ | [infra] | Nothing proven under load. |
| Capacity planning & sizing | ✗ | [ops] | |
| Caching strategy (beyond dedup) | ⚠ | [local] | No read-through cache / CDN. |
| Async where sync stacks latency | ✗ | [local] | Saga is synchronous. |
| Kafka partitions sized for throughput | ⚠ | [infra] | Fixed at 3 → parallelism ceiling. |

## H. Testing & quality
| Factor | Status | Effort | Note |
|---|---|---|---|
| Unit tests | ⚠ | [local] | 8 tests, behavior-asserting — thin. |
| Integration tests (Testcontainers) | ✗ | [local] | Proves it on real Postgres/Kafka. |
| Contract tests (Pact / Spring Cloud Contract) | ✗ | [local] | Catches the DTO-drift risk. |
| E2E tests | ✗ | [local] | |
| Coverage / mutation gates | ✗ | [local] | |
| Static analysis (Sonar/SpotBugs) | ✗ | [local] | |

## I. Operational maturity (Day-2)
| Factor | Status | Effort | Note |
|---|---|---|---|
| Runbooks / on-call / paging | ✗ | [ops] | |
| SLO / SLA + error budgets | ✗ | [ops] | |
| Incident mgmt + postmortems | ✗ | [ops] | |
| DLQ drain / data-fix procedures | ✗ | [ops] | |

## J. Governance, compliance & cost
| Factor | Status | Effort | Note |
|---|---|---|---|
| PCI-DSS (payment-service) | ✗ | [ops] | Yours is a simulated ledger — the right call; just say so. |
| PII / GDPR (user + order data) | ✗ | [ops] | Right-to-delete, residency. |
| Cost monitoring / FinOps + billing alarm | ✗ | [infra] | **Set a $1 billing alarm before any cloud deploy.** |
| License / SBOM compliance | ✗ | [local] | |

## K. API & contract management
| Factor | Status | Effort | Note |
|---|---|---|---|
| API versioning | ✗ | [local] | |
| OpenAPI / Swagger docs | ✗ | [local] | |
| Kafka schema registry + compat rules | ✗ | [infra] | The silent-deser-break risk. |
| Consumer-driven contracts | ✗ | [local] | |
| Deprecation policy | ✗ | [ops] | |

## L. Documentation & structure
| Factor | Status | Effort | Note |
|---|---|---|---|
| Architecture / review docs | ✓ | — | This doc + review + roadmap. |
| ADRs (decision records) | ✗ | [local] | |
| Reactor POM (parent build) | ✓ | [local] | R30 — `Spring/pom.xml` aggregates 10 modules; versions centralized. |
| Shared events module (kill duplicated DTOs) | ✓ | [local] | R31 — `events` module owns canonical `OrderPlaced`; 3 services depend on it. |

---

## Proposed implementation order (deployment-first)

The goal is a real cloud deployment, so we sequence toward "the whole thing runs in containers,"
then layer operations on top. Each phase is independently valuable.

**Phase 0 — Structural foundation [local]**
- R30 reactor/parent POM (one place for versions, one build).
- R31 shared `events` module (kill duplicated `OrderPlaced`/`StockRequest`/`ChargeRequest`).
- *Why first:* makes multi-module Docker builds sane and removes the silent-DTO-drift risk.

**Phase 1 — Containerize + run the whole stack locally [local]**
- Dockerfile per service (multi-stage, non-root, JRE-slim/distroless).
- `docker-compose.yml`: all 7 services + Postgres + Kafka + Redis, wired by env.
- Swap H2 → Postgres for every service; point Kafka/Redis at compose containers.
- *Milestone:* first time the entire system runs end-to-end together. Unlocks cloud.

**Phase 2 — Cloud deploy [infra]**
- Provision the target (Oracle Cloud Always Free recommended; AWS optional/paid).
- `k3s` (or docker-compose on a VM to start), deploy the images, ingress + TLS.
- Billing alarm FIRST.

**Phase 3 — Data & config hardening [infra/local]**
- R7 SKIP LOCKED outbox (now that Postgres + replicas exist).
- Secrets management; TLS in transit (R24 remainder); encryption at rest.

**Phase 4 — Observability [infra]**
- Tracing (Micrometer Tracing + OTel) w/ correlation IDs across hops.
- Prometheus + Grafana; centralized logs; a couple of SLO alerts.

**Phase 5 — Testing depth + CI/CD [local]**
- Testcontainers integration tests; contract tests; coverage gate.
- GitHub Actions: build → test → image → (deploy); R25 dependency scanning.

**Phase 6 — Scale & polish [infra/ops]**
- Kafka partitions + schema registry; async saga where it helps; load test → tune.
- HPA/autoscaling; runbooks; API versioning + OpenAPI.

**Deferred to frontend work:** R41 CORS/CSRF, security headers.
