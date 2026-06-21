# order-service — capstone learning log

This ONE project grows through the whole roadmap (see `../INTERVIEW-ROADMAP.md`).
We add a layer per step. I jot what we did + the interview takeaway here as we go.

## Step 1 — Spring MVC  ✅ DONE (2026-06-14)
- [x] Project scaffolded: Spring Boot 3.3.5, Java 17, `spring-boot-starter-web` + validation.
- [x] First controller — GET /hello (DispatcherServlet flow in action)
- [x] Product record + endpoints: GET /product/{id} (@PathVariable), GET /product?name= (@RequestParam), POST /product (@RequestBody)
- [x] ResponseEntity + status codes (200 / 201+Location / 404 / 400)
- [x] Validation (@Valid + @NotBlank/@Positive) → automatic 400
- [x] Global @RestControllerAdvice → clean field-level error JSON
- [x] Service layer (controller → @Service), constructor injection, thin controller
- [x] @WebMvcTest + MockMvc + @MockBean — 3 tests green
- [ ] (deferred for later) interceptor/correlation-id, content negotiation/XML, multipart, async — covered in roadmap, revisit if time

### Takeaways (interview one-liners)
- @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
- Every request → DispatcherServlet (front controller) → HandlerMapping → HandlerAdapter → converter/ViewResolver.
- @ResponseBody / @RestController → skip the view, write return value to the body via an HttpMessageConverter (Jackson for JSON). String→text/plain, object→JSON.
- @PathVariable identifies the resource (/product/5); @RequestParam filters/options (?name=x).
- ResponseEntity = body + status + headers. 201 Created SHOULD carry a Location header → ResponseEntity.created(uri).
- @RestControllerAdvice = @ControllerAdvice + @ResponseBody; global @ExceptionHandler. Same as ControllerAdvice IF you return ResponseEntity.
- @Transactional boundary belongs on the SERVICE, not the controller (link to Step 4).
- @WebMvcTest = web slice + MockMvc + @MockBean(service). @DataJpaTest = repo slice. @SpringBootTest = full integration.

## Step 2 — Spring AOP  ✅ DONE
- [x] Added spring-boot-starter-aop.
- [x] @Around LoggingAspect — logs args/return/elapsed; narrowed pointcut to service-only.
- [x] Over-broad pointcut pitfall observed (whole-app pointcut advised controller+service → nested logs).
- [x] @Audited custom annotation + @annotation(audited) pointcut + @AfterReturning AuditAspect (surgical targeting).
- [x] Self-invocation gotcha: this.create() inside createBatch bypassed proxy → no advice/audit.
      Fixes: (1) self-inject proxy with @Lazy, (2) extract to separate bean (preferred), (3) AopContext (last resort).
- [x] JDK dynamic proxy vs CGLIB — proved via AopUtils; ProductService = CGLIB ($$SpringCGLIB$$) (no interface).
      Boot defaults proxyTargetClass=true. final class/method can't be proxied. (ProxyInspector was temporary.)

### AOP takeaways so far
- @Aspect + @Component both required; AOP starter auto-enables @EnableAspectJAutoProxy.
- @Around wraps; pjp.proceed() runs the target; MUST return result; rethrow exceptions.
- Advice fires because controller→service are DIFFERENT beans (call goes through the proxy).
  Self-invocation (same-bean call) bypasses the proxy → advice won't fire.
- Keep pointcuts targeted; over-broad = perf/noise/accidental advising.
- final service class → CGLIB can't proxy → advice silently skipped.

## Step 3 — Spring JDBC  ✅ DONE (light, by choice — JPA is the real-world default)
- [x] Built a JdbcTemplate repo hands-on (RowMapper, KeyHolder, EmptyResultDataAccessException) then
      decided NOT to wire it — went to JPA instead. Concept covered; interview one-liners banked.
- Key points: JdbcTemplate removes boilerplate/leaks; DataAccessException (unchecked) translation;
  use JDBC for tuned SQL/bulk/reporting; JdbcClient (6.1+) is the modern fluent successor.

## Step 5 — Spring Data JPA (basics)  ✅ DONE
- [x] Added spring-boot-starter-data-jpa + H2; ddl-auto=create-drop; show-sql.
- [x] ProductEntity (@Entity/@Table/@Id/@GeneratedValue IDENTITY); Product stays as the DTO (separation).
- [x] ProductRepository = interface extends JpaRepository<ProductEntity,Long> + derived findByNameIgnoreCase.
- [x] ProductService maps entity<->DTO. VERIFIED LIVE: POST 201, GET 200/404, Hibernate SQL in logs.
- Payoff: JpaRepository replaced all the JdbcTemplate/RowMapper/SQL boilerplate.

### Heavy JPA — relationships + N+1  ✅ DONE
- [x] OrderEntity 1—* OrderItem *—1 ProductEntity. @OneToMany(mappedBy) inverse vs @ManyToOne+@JoinColumn owning.
      protected no-arg ctor (JPA reflection/proxies + keeps entities valid). addItem() syncs both sides. @Table("orders").
- [x] create-order endpoint (OrderController/OrderService/OrderRepository + OrderDtos request/response).
- [x] Reproduced N+1: GET /orders → 5 queries for 2 orders (1 orders + N items + M products; product cached via 1st-level cache).
- [x] Fix 1 JOIN FETCH (1 join query, distinct, left join) — VERIFIED 5→1.
- [x] Fix 2 @EntityGraph(attributePaths) on findAll() — declarative, same join.
- [x] Fix 3 @BatchSize / default_batch_fetch_size=10 — 1+few via IN clause; pagination-safe.
- Decision: JOIN FETCH/@EntityGraph for single fetch (can't paginate collections, HHH000104); @BatchSize for paginated lists.

### JPA — pagination, locking, OSIV  ✅ DONE
- [x] Pagination: Page (2 queries incl count, knows totalPages) vs Slice (1 query, no count, hasNext via size+1).
      GOTCHA learned: repository.findAll(Pageable) ALWAYS returns Page (count) — declaring the SERVICE method
      as Slice doesn't help (Page IS-A Slice); need a repo method that itself returns Slice (@Query findAllSliced).
- [x] @Version optimistic locking: UPDATE ... WHERE id=? AND version=? bumps version; stale version → 0 rows →
      OptimisticLockingFailureException → mapped to 409 in GlobalExceptionHandler. PUT /product/{id} demo.
      Optimistic (web default) vs pessimistic (@Lock PESSIMISTIC_WRITE / SELECT FOR UPDATE) for hotspots.
- [x] OSIV: spring.jpa.open-in-view=false. Demoed LazyInitializationException on detached entity (itemcount-bad)
      → fix = access inside @Transactional (or fetch/DTO). OSIV holds DB connection for whole request + hides N+1.
- **JPA (Step 5) COMPLETE.**

## Step 4 — Transactions  ✅ DONE
- [x] @Transactional on ProductBatchService.createBatch → atomic batch (duplicate name → whole batch rolls back).
- [x] Checked-exception gotcha: default rolls back only on RuntimeException/Error; checked commits →
      fixed with @Transactional(rollbackFor = Exception.class). (BusinessException + /product/checked)
- [x] Propagation REQUIRES_NEW: AuditLogService.log() commits in its own tx → audit survives outer rollback.
      (AuditLogEntity/Repository/Service, /product/audit-demo, GET /audit)
- Key points: @Transactional is AOP (same self-invocation limit); put it on the SERVICE; readOnly for queries;
  REQUIRES_NEW uses a separate connection (pool-exhaustion risk).

## Step 6 — Spring Security  (IN PROGRESS — ~70%)
- [x] Added spring-boot-starter-security → saw everything auto-locked (default user + generated password).
- [x] Understood the FILTER CHAIN end-to-end: DelegatingFilterProxy → FilterChainProxy →
      SecurityContextHolderFilter → CsrfFilter → (auth filter) → ExceptionTranslationFilter → AuthorizationFilter → DispatcherServlet.
      401 = AuthenticationException (who are you), 403 = AccessDeniedException (not allowed); both via ExceptionTranslationFilter.
- [x] Own SecurityFilterChain (SecurityConfig): permit GET /product/** + /h2-console + /auth/**; anyRequest authenticated; httpBasic; csrf disabled; frameOptions sameOrigin.
- [x] Own users: InMemoryUserDetailsManager (hemanth/USER, admin/ADMIN) + BCryptPasswordEncoder → default generated password gone.
- [x] Test fix: @AutoConfigureMockMvc(addFilters=false) on @WebMvcTest.
- [x] JWT 3a (login→token): jjwt 0.12.6; JwtService (builder=create / parser=read); AuthController /auth/login uses AuthenticationManager → issues signed JWT. VERIFIED.
- DEBUG note resolved: a "no-auth POST not 401" was Postman sending stale auth; server returns 401 correctly (verified via curl).

### RESUME — finish Security (3b onward)
1. **Build JwtAuthenticationFilter** (OncePerRequestFilter): read Bearer header → JwtService.isValid → load user → set SecurityContextHolder. Permissive (no/invalid token → just continue; AuthorizationFilter rejects later).
2. Wire it: SecurityConfig → sessionManagement STATELESS + http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).
3. Test: /auth/login → token → send Authorization: Bearer <token> → access secured POST without password.
4. Method security: @EnableMethodSecurity + @PreAuthorize (admin-only endpoint) → 403 demo.
5. (prod aside) OAuth2 Resource Server — validate an IdP's JWTs via jwk-set-uri (conceptual/short demo).
Then **Step 7 (Spring Boot internals)** + **Step 8 (Reactive/WebFlux)**.

## How to run
```
cd d:\work\test\Spring\order-service
mvn spring-boot:run
```
Then hit http://localhost:8080/... in a browser or with curl.
