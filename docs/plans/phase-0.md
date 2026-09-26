# Phase 0 plan - Project setup and roles

## Context

`docs/PROGRESS.md` has no entries and the repository contains only `CLAUDE.md`, `README.md` and `docs/`. Phase N is therefore **Phase 0** of `docs/BUILD_PLAN.md`: monorepo skeleton, MySQL via Docker, Flyway baseline, session login + CSRF cookie, `users` with a `role` enum and one seeded account per role, `settings` + typed `SettingsService`, `audit_logs` + `AuditService`, `DomainException` hierarchy + `ProblemDetail` advice, injected `Clock` + mutable test clock, `AbstractIntegrationTest` on Testcontainers MySQL, and a frontend with login, role-based layout and an API client with CSRF handling.

Done when: each role can log in and sees only its own menu; a student calling an admin endpoint gets 403. Tests: role access tests for every role against real endpoints.

Nothing from Phase 1+ is built. Menu entries for later phases are placeholders so the "sees only its own menu" check is demonstrable.

## Blocking prerequisites (machine setup, not code)

Checked on this machine on 2026-09-26:

| Item | State | Action |
|---|---|---|
| JDK 21 | not installed, no `JAVA_HOME` | `winget install --id EclipseAdoptium.Temurin.21.JDK -e`, open a new terminal, check `java -version` |
| Maven | not installed | not needed: the plan adds the Maven Wrapper, which downloads Maven itself |
| Docker Desktop | installed, daemon not running | start Docker Desktop before `docker compose up -d` and before `./mvnw test` (Testcontainers) |
| Node / npm | v24 / 12 | ok |
| Git branch | literally `phase-N` | rename before the PR: `git branch -m phase-N phase-0` |

Installing the JDK changes the machine, so the team member running the session does it (or explicitly approves me running winget).

## Versions (verified against Maven Central, npm and the Spring docs on 2026-09-26)

- **Spring Boot 3.5.16** (Java 21): the final 3.x release (2026-06-25). It brings Spring Security 6.5.11, Hibernate 6.6.53, Flyway 11.7.2, Connector/J 9.7.0, Testcontainers 1.21.x. start.spring.io now offers only 4.0.8 / 4.1.1, so the pom is written by hand. See Ambiguity 1.
- MySQL `8.4` (LTS) for docker-compose and Testcontainers. Testcontainers 1.21.4 is compatible with 8.4 (its bundled `my.cnf` breaks only on MySQL 9.x); Flyway 11.7.2 logs an "untested version" warning on 8.4 but works. Do not move to 9.x.
- Maven Wrapper 3.3.4 "only-script" distribution + Apache Maven 3.9.16.
- Frontend: Vite 8 react-ts template (React 19.3), `react-router@8` (data mode; `react-router-dom` no longer exists, `RouterProvider` comes from `react-router/dom`, the rest of the data API is unchanged from v7), `@tanstack/react-query@5`, `tailwindcss@4` + `@tailwindcss/vite`.

---

## 1. Tests written first (all with @DisplayName)

Phase 0 has **no BR-01..BR-11 rule**. The tests written first are the role-access tests BUILD_PLAN asks for, plus one test class per infrastructure piece the later phases will rely on. Display names carry a tag (`AUTH`, `CSRF`, `ROLE`, `SETTINGS`, `AUDIT`, `ERROR`, `DEMO`, `INFRA`) in the same spirit as the `BR-xx` prefixes. All classes extend `AbstractIntegrationTest` (real MySQL, real HTTP on a random port) unless marked unit. Class names end in `Test` so plain `./mvnw test` runs them.

### `common/security/AuthFlowTest`
- `"AUTH anonymous GET /api/me returns 401 problem+json and sets a readable XSRF-TOKEN cookie"`
- `"AUTH login as {0} returns the user and GET /api/me reports the same role"` (`@ParameterizedTest` + `@EnumSource(Role.class)`: STUDENT, ADMIN, TECHNICIAN, ACCOUNTANT, AFFAIRS)
- `"AUTH login with a wrong password returns 401 and no JSESSIONID"`
- `"AUTH a disabled account cannot log in"`
- `"AUTH login sets an HttpOnly SameSite=Lax JSESSIONID, changes the session id and rotates the XSRF-TOKEN cookie"`
- `"AUTH POST /api/auth/logout returns 204 and GET /api/me returns 401 afterwards"`

### `common/security/CsrfProtectionTest`
- `"CSRF POST /api/auth/login without X-XSRF-TOKEN is rejected with 403"`
- `"CSRF POST with a header that does not match the cookie is rejected with 403 even with a valid session"`
- `"CSRF GET never requires the token"`

### `common/security/RoleAccessTest`
- `"ROLE {0} gets 403 problem+json on GET /api/settings"` (parameterized over STUDENT, TECHNICIAN, ACCOUNTANT, AFFAIRS)
- `"ROLE ADMIN gets 200 on GET /api/settings and PUT /api/settings/offer_hours changes the value"`
- `"ROLE STUDENT PUT /api/settings/offer_hours returns 403 and leaves the value unchanged"`

### `common/settings/SettingsServiceTest`
- `"SETTINGS V2 seeds offer_hours=48, warning_threshold=3 and repair_due_hours urgent=24 normal=72 low=168"`
- `"SETTINGS typed getters return int, long and Duration values and update() is visible to the next read"`
- `"SETTINGS update() with a value that does not parse for the key type throws InvalidSettingValueException (422)"`
- `"SETTINGS update() of an unknown key throws NotFoundException (404)"`
- `"SETTINGS reading a key whose row was deleted throws IllegalStateException (configuration error)"`

### `common/audit/AuditServiceTest`
- `"AUDIT record() stores subject, from/to status, the current user id and the clock instant"`
- `"AUDIT record() with no authenticated user stores a null user id"`
- `"AUDIT record() outside a transaction fails with IllegalTransactionStateException (Propagation.MANDATORY)"`
- `"AUDIT advancing the MutableClock by 3 days changes created_at of the next row"` (proves the test clock replaces the production `Clock` bean)

### `common/error/ApiExceptionHandlerTest` (via test-only `ErrorProbeController` under `/api/test/errors/*`)
- `"ERROR InvalidStateTransitionException maps to 409 application/problem+json"`
- `"ERROR BusinessRuleViolationException maps to 422 and carries the rule id"`
- `"ERROR NotFoundException maps to 404"`
- `"ERROR @Valid failure maps to 400 with field errors"`
- `"ERROR an unexpected exception maps to 500 without internals"`

### `demo/DemoDataServiceTest`
- `"DEMO seedIfEmpty() on an empty database creates exactly one enabled account per role"`
- `"DEMO seedIfEmpty() is idempotent: a second run creates nothing"`
- `"DEMO every seeded account can log in through POST /api/auth/login"`

### `DmsApplicationTest`
- `"INFRA context starts with Flyway V1 and V2 applied and Hibernate ddl-auto=validate passing"`

### `support/MutableClockTest` (unit)
- `"INFRA MutableClock advance() moves instant() forward and reset() returns to the initial instant"`

---

## 2. Files to create or change

### Root
- `.gitignore` (new): `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `.idea/`, `*.iml`, `.vscode/`, `*.log`, `.env*`
- `docker-compose.yml` (new): `mysql:8.4`, database `dms`, user `dms` / `dms`, root `root`, port 3306, named volume, healthcheck `mysqladmin ping`
- `README.md` (change): prerequisites, the four commands from CLAUDE.md, demo accounts
- `CLAUDE.md` (change, one line under Code conventions, see Decisions): enum fields need `@JdbcTypeCode(SqlTypes.VARCHAR)`

### Backend `backend/`
- `pom.xml` (new): parent `spring-boot-starter-parent` 3.5.16, `java.version` 21. Deps: `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`, `flyway-core`, `flyway-mysql`, `mysql-connector-j` (runtime). Test: `spring-boot-starter-test`, `spring-security-test`, `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:mysql`. No Lombok, no H2.
- `mvnw`, `mvnw.cmd` (new): from `https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/3.3.4/maven-wrapper-distribution-3.3.4-only-script.zip`; then `git update-index --chmod=+x backend/mvnw`
- `.mvn/wrapper/maven-wrapper.properties` (new): `wrapperVersion=3.3.4`, `distributionType=only-script`, `distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip`
- `src/main/resources/application.yml` (new): `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`, Flyway enabled, `server.servlet.session.cookie.http-only=true`, `same-site=lax`, `server.error.include-stacktrace=never`, `dms.clock.zone=Asia/Ho_Chi_Minh`. No datasource here (tests get it from `@ServiceConnection`).
- `src/main/resources/application-dev.yml` (new): `jdbc:mysql://localhost:3306/dms?characterEncoding=UTF-8`, `dms` / `dms`, SQL logging
- `src/main/resources/db/migration/V1__baseline.sql` (new): `users`, `settings`, `audit_logs` (schema in Design notes)
- `src/main/resources/db/migration/V2__seed_settings.sql` (new): the five defaults of Ambiguity 4

Java, package `vn.edu.hust.dms`:
- `DmsApplication.java`
- `common/config/ClockConfig.java` - `Clock` bean, zone from `dms.clock.zone`
- `common/user/UserAccount.java` (entity on table `users`; named `UserAccount` to avoid clashing with Spring Security's `User`), `common/user/Role.java` (enum), `common/user/UserAccountRepository.java`
- `common/security/SecurityConfig.java` - filter chain, CSRF cookie repository + SPA handler, entry point / denied handler, `@EnableMethodSecurity`, `PasswordEncoder`, `AuthenticationManager`, `SecurityContextRepository`, login `SessionAuthenticationStrategy`
- `common/security/SpaCsrfTokenRequestHandler.java`
- `common/security/ProblemDetailAuthenticationEntryPoint.java` (401 `application/problem+json`), `common/security/ProblemDetailAccessDeniedHandler.java` (403, also serves CSRF failures)
- `common/security/UserPrincipal.java` (`UserDetails` with id, username, role, enabled; authority `ROLE_<role>`), `common/security/DmsUserDetailsService.java`, `common/security/CurrentUserService.java` (`Optional<UserPrincipal> current()`, `Optional<Long> currentUserId()`)
- `common/security/web/AuthController.java` (`POST /api/auth/login`, `GET /api/me`), `common/security/web/LoginRequest.java`, `common/security/web/MeResponse.java` (records). Logout is Spring Security's `LogoutFilter` on `POST /api/auth/logout` (204)
- `common/settings/Setting.java` (entity), `common/settings/SettingKey.java` (enum: key string + value type INTEGER / LONG / HOURS), `common/settings/SettingRepository.java`, `common/settings/SettingsService.java` (`getInt`, `getLong`, `getDuration`, `all`, `update`), `common/settings/InvalidSettingValueException.java` (422)
- `common/settings/web/SettingsController.java` (class-level `@PreAuthorize("hasRole('ADMIN')")`, `GET /api/settings`, `PUT /api/settings/{key}`), `common/settings/web/SettingResponse.java`, `common/settings/web/UpdateSettingRequest.java`
- `common/audit/AuditLog.java` (entity), `common/audit/AuditSubjectType.java` (enum), `common/audit/AuditLogRepository.java`, `common/audit/AuditService.java`
- `common/error/DomainException.java` (abstract, carries `HttpStatus`), `common/error/InvalidStateTransitionException.java` (409), `common/error/BusinessRuleViolationException.java` (422, `ruleId` such as `BR-03` emitted as ProblemDetail property `rule`), `common/error/NotFoundException.java` (404), `common/error/ApiExceptionHandler.java` (`@RestControllerAdvice extends ResponseEntityExceptionHandler`)
- `demo/DemoAccount.java` (enum: username, full name, role; shared demo password), `demo/DemoDataService.java` (`@Transactional boolean seedIfEmpty()`), `demo/DemoDataSeeder.java` (`@Profile("demo")` `ApplicationRunner` calling the service)

Tests, `backend/src/test/java/vn/edu/hust/dms`:
- `support/AbstractIntegrationTest.java` - `@SpringBootTest(RANDOM_PORT)`, `@Import(TestClockConfig.class)`, `@ServiceConnection static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")` started in a static block (no `@Testcontainers` / `@Container`, which would stop it after each class), `@BeforeEach` database clean + clock reset + `SecurityContextHolder.clearContext()`
- `support/MutableClock.java`, `support/TestClockConfig.java` (`@TestConfiguration`, `@Bean @Primary MutableClock testClock()`, bean name differs from `clock` because bean overriding stays off)
- `support/DatabaseCleaner.java` - `SET FOREIGN_KEY_CHECKS=0`, truncate every base table except `flyway_schema_history`, re-run `classpath:db/migration/V2__seed_settings.sql` with `ScriptUtils`
- `support/TestUsers.java` - creates an enabled or disabled user for a role through `UserAccountRepository` + `PasswordEncoder`, returns the credentials
- `support/ApiClient.java` - `RestClient` on the random port over a JDK `HttpClient` with a `CookieManager` (automatic cookie jar), adds `X-XSRF-TOKEN` from the stored cookie on non-GET, never throws on 4xx/5xx, `loginAs(username, password)` does `GET /api/me` then `POST /api/auth/login`; one client per test = one browser
- `support/ErrorProbeController.java` - test-only `@RestController` under `/api/test/errors/*` throwing each exception type (picked up because test classes share the scanned base package)
- `src/test/resources/application-test.yml` - quieter logging only
- the nine test classes of section 1

### Frontend `frontend/`
- scaffold with `npm create vite@latest frontend -- --template react-ts`, then `npm i react-router @tanstack/react-query tailwindcss @tailwindcss/vite`; delete the template `App.css`, `App.tsx` demo content and `assets/`
- `package.json`, `vite.config.ts` (react plugin, `tailwindcss()` plugin, `server.proxy['/api'] = 'http://localhost:8080'`), `tsconfig*.json`, `eslint.config.js`, `index.html` (title "DMS")
- `src/main.tsx` - `QueryClientProvider` + `RouterProvider` (from `react-router/dom`)
- `src/index.css` - `@import "tailwindcss";`
- `src/app/router.tsx` - `/login`; `/` = `RequireAuth` > `AppLayout` with index `HomePage` and one placeholder route per menu entry wrapped in `RequireRole`; `/forbidden`; `*`
- `src/app/queryClient.ts`
- `src/api/client.ts` - `apiFetch<T>()`: same-origin fetch, JSON, `ensureCsrfToken()` (does `GET /api/me` first when the `XSRF-TOKEN` cookie is missing), sends `X-XSRF-TOKEN` on non-GET, parses `application/problem+json` into `ApiError { status, title, detail, errors?, rule? }`, 401 clears the `me` query
- `src/api/auth.ts` - `login()`, `logout()`, `fetchMe()`; `src/api/types.ts` - `Role`, `Me`, `ProblemDetail`
- `src/auth/AuthProvider.tsx` (`useQuery(["me"])`, retry off, 401 = signed out), `src/auth/useAuth.ts`, `src/auth/RequireAuth.tsx` (redirect to `/login` remembering the target), `src/auth/RequireRole.tsx` (renders `ForbiddenPage`)
- `src/layout/AppLayout.tsx`, `src/layout/Sidebar.tsx` (items from `navigation.ts` filtered by role, user name + role, logout), `src/layout/navigation.ts` (`NAV_ITEMS: { label, to, roles: Role[], phase }[]`, proposal in Ambiguity 7)
- `src/pages/LoginPage.tsx`, `src/pages/HomePage.tsx` ("Signed in as ... (ROLE)"), `src/pages/PlaceholderPage.tsx` ("Coming in Phase N"), `src/pages/ForbiddenPage.tsx`, `src/pages/NotFoundPage.tsx`
- `src/lib/cookies.ts` - `readCookie(name)`

### Docs
- `docs/PROGRESS.md` (append at close-out): built, tests passing (no BR tests in Phase 0; list the tagged suites), decisions, open questions
- `docs/plans/phase-0.md` (new at close-out): copy of this approved plan (WORKFLOW step 6)

---

## 3. Ambiguities the team must decide

Each comes with the default I will use if nobody objects.

1. **Spring Boot line.** CLAUDE.md says "Spring Boot 3"; 3.5.16 is the last 3.x release and its OSS support ended June 2026; Initializr only offers 4.x. Default: **3.5.16** (matches the spec, stable and well-documented). Choosing 4.x changes the pom, Jackson 3 packages and Spring Security 7 details and means updating CLAUDE.md.
2. **Login identifier and seed credentials.** Default: login by `username`; accounts `admin`, `student`, `technician`, `accountant`, `affairs`, all with password `password`, Vietnamese full names (student `Nguyễn Văn An`). Alternative: login by email.
3. **`users` vs `students`.** Default: `users` holds only login identity (username, password hash, full name, email, role, enabled). Student data (student code, gender, GPA, ...) goes in a `students` table referencing `users.id` in Phase 2. Confirm so the Phase 0 schema needs no rework.
4. **Which settings to seed now.** Default: only keys CLAUDE.md explicitly calls settings: `offer_hours=48`, `warning_threshold=3`, `repair_due_hours_urgent=24`, `repair_due_hours_normal=72`, `repair_due_hours_low=168`. Electricity/water prices, rent, invoice due days (Phase 4) and an optional `debt_block_days=60` (BR-09) are added by later migrations once the team supplies values. Alternative: seed placeholder prices now.
5. **Application time zone.** Default: `Clock` in `Asia/Ho_Chi_Minh`; instants persisted as UTC `DATETIME(6)`, calendar dates as `DATE`. This fixes month/day boundaries for BR-05, BR-07 and BR-10 later.
6. **Settings API in Phase 0.** BUILD_PLAN only asks for a `SettingsService`. Default: also expose ADMIN-only `GET /api/settings` and `PUT /api/settings/{key}` (no UI page yet) so the 403 check has a real admin endpoint and later phases can tune settings without SQL. Alternative: no API; use another admin endpoint for the 403 test.
7. **Role menus (placeholders).** Default:
   - STUDENT: Home, My application, My room & contract, Invoices, Repair tickets
   - ADMIN: Home, Facilities, Admission rounds, Applications, Bed assignment, Contracts, Meter readings, Repair tickets, Violations, Settings, Audit log
   - ACCOUNTANT: Home, Invoices, Payments
   - TECHNICIAN: Home, My tickets
   - AFFAIRS: Home, Eviction proposals, Violations
   BUILD_PLAN Phase 4 has the Admin recording meter readings, while WORKFLOW's design prompt gives that screen to the Accountant. Only affects Phase 4; the placeholder sits under ADMIN for now.
8. **Where the five accounts are seeded.** Default: `DemoDataSeeder` (`demo` profile) per CLAUDE.md; tests create their own users. Consequence: a database started without the `demo` profile has no accounts at all. Alternative: a Flyway migration that also creates a bootstrap `admin` everywhere.
9. **`audit_logs` columns.** Default: exactly the six columns of CLAUDE.md. Phases that need a free-text reason (reject reason, meter override) add a nullable column by migration. Settings changes are not audited (`subject_id` is numeric, settings have a string key).
10. **Branch name.** The branch is literally `phase-N`; WORKFLOW expects `phase-0`. I will not rename it; the team runs `git branch -m phase-N phase-0` before the PR.

**Decisions taken without asking (routine):**
- Every enum field is mapped `@Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)`. Hibernate 6.6 otherwise expects a native MySQL `ENUM` column and `ddl-auto=validate` fails against `VARCHAR`. Columns stay `VARCHAR` so later migrations add values freely. One line is added to CLAUDE.md's Code conventions so later phases follow it.
- `users.enabled` flag (needed by `UserDetails`, default true) and a nullable `email` (Phase 6 emails notifications).
- Settings columns are `setting_key` / `setting_value` because `key` is reserved in MySQL.
- Password hashes use `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt with the `{bcrypt}` prefix).
- Session timeout stays at the servlet default (30 minutes idle), configurable in `application.yml`.
- No Lombok; DTOs are records; entities have explicit getters.

---

## Design notes

### Security
- One `SecurityFilterChain`: CSRF with `CookieCsrfTokenRepository.withHttpOnlyFalse()` and `SpaCsrfTokenRequestHandler` (verified against the Spring Security 6.5 reference, "Configure CSRF for Single-Page Applications": `handle()` delegates to `XorCsrfTokenRequestAttributeHandler` then calls `csrfToken.get()`; `resolveCsrfTokenValue()` uses the plain handler when the `X-XSRF-TOKEN` header is present). Because `get()` runs on every request, the cookie is written on every response, including the anonymous 401 from `/api/me`, so the SPA needs no dedicated CSRF endpoint.
- `authorizeHttpRequests`: `POST /api/auth/login` permitAll, `dispatcherTypeMatchers(ERROR)` permitAll, everything else authenticated. `formLogin`, `httpBasic` and `requestCache` disabled. `exceptionHandling` uses the two ProblemDetail handlers (401 "Authentication required", 403 "Access denied"). CSRF failures come out of `CsrfFilter` through the `AccessDeniedHandler`, so they are 403 problem JSON too. Logout: `POST /api/auth/logout`, `HttpStatusReturningLogoutSuccessHandler(204)`, invalidates the session and deletes `JSESSIONID`.
- Login controller (Spring Security 6 makes the authentication mechanism responsible for invoking the `SessionAuthenticationStrategy`): `authenticationManager.authenticate(...)`, then `loginSessionStrategy.onAuthentication(auth, request, response)` where the strategy is `CompositeSessionAuthenticationStrategy(ChangeSessionIdAuthenticationStrategy, CsrfAuthenticationStrategy(cookieRepo) with setRequestHandler(spaHandler))` (session fixation protection + CSRF token rotation), then create the context, set it on the holder strategy and `securityContextRepository.saveContext(...)` (creates the session and the `JSESSIONID`). Returns `MeResponse(id, username, fullName, role)`. `BadCredentialsException` / `DisabledException` map to 401 with a generic detail.
- Authorities are `ROLE_<role>`; `@PreAuthorize("hasRole('ADMIN')")` on `SettingsController` only (service getters stay open because scheduled jobs call them without a user). A denial from the method-security proxy surfaces inside `DispatcherServlet` as `AuthorizationDeniedException` (an `AccessDeniedException`), so `ApiExceptionHandler` maps `AccessDeniedException` to 403 problem JSON, giving the same body as the filter-level 403.

### Errors
- `DomainException` (abstract, `HttpStatus status()`), subclasses 409 / 422 / 404 as listed. `ApiExceptionHandler extends ResponseEntityExceptionHandler` (Boot's `ProblemDetailsExceptionHandler` then backs off; `@Valid` failures already become 400 ProblemDetail and `handleMethodArgumentNotValid` is overridden to add `errors: [{field, message}]`), plus handlers for `DomainException`, `AuthenticationException` (401), `AccessDeniedException` (403) and a catch-all 500 that never exposes the message. Spring picks the most specific handler, so the catch-all never swallows a denial.

### Clock
- `ClockConfig`: `Clock.system(ZoneId.of(dms.clock.zone))`. Test side: `MutableClock` (`set(Instant)`, `advance(Duration)`, `advanceDays`, `reset`) registered `@Primary` by `TestClockConfig`; `AbstractIntegrationTest` resets it to a fixed instant before each test. Time-based code only ever injects `Clock`.

### Schema (V1) and seed (V2)
```
users        id BIGINT PK AUTO_INCREMENT, username VARCHAR(64) UNIQUE, password_hash VARCHAR(100), full_name VARCHAR(128),
             email VARCHAR(128) NULL UNIQUE, role VARCHAR(20), enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at DATETIME(6)
settings     setting_key VARCHAR(64) PK, setting_value VARCHAR(255) NOT NULL, description VARCHAR(255) NULL, updated_at DATETIME(6)
audit_logs   id BIGINT PK AUTO_INCREMENT, subject_type VARCHAR(40), subject_id BIGINT, from_status VARCHAR(30) NULL,
             to_status VARCHAR(30), user_id BIGINT NULL FK users(id), created_at DATETIME(6), INDEX (subject_type, subject_id)
```
All tables `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci` (Vietnamese names). V2 inserts the five settings of Ambiguity 4. `ddl-auto=validate` fails startup if an entity and the migrations drift, which `DmsApplicationTest` catches first.

### Settings
- `SettingKey` enum (`OFFER_HOURS("offer_hours", HOURS)`, `WARNING_THRESHOLD("warning_threshold", INTEGER)`, the three `REPAIR_DUE_HOURS_*`). `SettingsService` reads the row on every call (no cache; tests change values), `getInt` / `getLong` / `getDuration` by key; a missing row is a configuration error (`IllegalStateException`); `update(String key, String value)` resolves the key (`NotFoundException`) and validates by type (`InvalidSettingValueException`, 422).

### Audit
- `AuditService.record(AuditSubjectType, long subjectId, String from, String to)` plus an enum overload, `@Transactional(propagation = MANDATORY)` so a transition can never audit outside its own transaction. `user_id` from `CurrentUserService` (null for system and scheduled actions), `created_at = clock.instant()`. `AuditSubjectType` lists the aggregates of CLAUDE.md's domain model (APPLICATION, BED_OFFER, CONTRACT, INVOICE, REPAIR_TICKET, EVICTION_PROPOSAL, USER); Phase 0 tests use `APPLICATION` with a fake id (no FK).

### Demo data
- `DemoAccount` enum holds the five accounts; `DemoDataService.seedIfEmpty()` creates them when `users` is empty and logs the credentials once at INFO; `DemoDataSeeder` is the `demo`-profile runner. Phase 1 extends the service with buildings and rooms.

### Test infrastructure
- One static `MySQLContainer` (`mysql:8.4`) in `AbstractIntegrationTest` with `@ServiceConnection`. Verified in the Boot 3.5.16 source: `@ServiceConnection` fields are collected from superclasses and must be static, and Boot starts the container lazily; the explicit `start()` in a static block is harmless. The Spring context is cached, so container and context start once per `./mvnw test`. Optional speed-up: `withReuse(true)` plus `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`.
- Tests are not `@Transactional`: the server handles requests on other threads, and later phases need committed data for the locking tests. `DatabaseCleaner` runs before each test.
- HTTP goes through `ApiClient` (RestClient on the random port) rather than MockMvc: MockMvc runs the filter chain but no Tomcat, so `HttpOnly` / `SameSite` cookie attributes and the real cookie round-trip the SPA depends on would go untested.

### Frontend flow
- On load `AuthProvider` runs `fetchMe()`; 401 means signed out (and the XSRF cookie now exists). `LoginPage` posts to `/api/auth/login` with the header read from the cookie, then invalidates `me`. `Sidebar` filters `NAV_ITEMS` by `me.role`; each entry except Home routes to `PlaceholderPage`. `RequireRole` shows `ForbiddenPage` when a role types a URL it should not see, mirroring the backend 403 (which stays the real enforcement).

---

## Verification

1. Prerequisites: JDK 21 on PATH, Docker Desktop running (`docker info` succeeds).
2. `docker compose up -d` (root).
3. Backend suite: Git Bash `cd backend && ./mvnw test`; PowerShell `cd backend; .\mvnw.cmd test`. All tests of section 1 green on real MySQL; nothing skipped or disabled.
4. Run the app: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo` (PowerShell: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,demo"`). The log shows Flyway V1 + V2 applied and the demo accounts created.
5. Frontend: `cd frontend && npm install && npm run build && npm run lint` pass; `npm run dev`, open http://localhost:5173.
6. Manual "Done when": log in as each of the five accounts; each sees only its own menu; logout works.
7. 403 proof from a terminal (Git Bash; in PowerShell use `curl.exe`). The XSRF token is re-read after login because login rotates it:
```
curl -si -c cj.txt http://localhost:8080/api/me                                  # 401 problem+json, Set-Cookie XSRF-TOKEN
X=$(grep XSRF-TOKEN cj.txt | awk '{print $7}')
curl -si -b cj.txt -c cj.txt -H "X-XSRF-TOKEN: $X" -H "Content-Type: application/json" \
     -d '{"username":"student","password":"password"}' http://localhost:8080/api/auth/login   # 200, JSESSIONID HttpOnly SameSite=Lax
curl -si -b cj.txt http://localhost:8080/api/me                                  # 200 ... "role":"STUDENT"
curl -si -b cj.txt http://localhost:8080/api/settings                            # 403 application/problem+json
X=$(grep XSRF-TOKEN cj.txt | awk '{print $7}')
curl -si -b cj.txt -c cj.txt -X POST -H "X-XSRF-TOKEN: $X" http://localhost:8080/api/auth/logout   # 204
curl -si -b cj.txt http://localhost:8080/api/me                                  # 401
```

## Close-out (WORKFLOW step 6)

Append the Phase 0 entry to `docs/PROGRESS.md`, copy this plan to `docs/plans/phase-0.md`, make sure `backend/mvnw` is committed executable, commit "Phase 0: project setup and roles". The team renames the branch to `phase-0`, pushes and opens the PR for another member to review.
