# DMS Progress Log

Claude Code appends one entry at the end of each phase. Newest entry at the bottom.

Each entry records:
- **Built:** what was implemented
- **BR tests passing:** list of BR IDs
- **Decisions:** choices made during the phase and why
- **Open questions:** anything the team must decide before the next phase

---

## Phase 0 - Project setup and roles (2026-09-26)

**Built**
- Monorepo: `backend/` (Spring Boot 3.5.16, Java 21, Maven Wrapper 3.3.4 only-script + Maven 3.9.16), `frontend/` (Vite 8, React 19, TypeScript 5.9, Tailwind 4, React Router 8, TanStack Query 5), `docker-compose.yml` (MySQL 8.4, db/user/password `dms`).
- Flyway `V1__baseline.sql` (`users`, `settings`, `audit_logs`) and `V2__seed_settings.sql` (`offer_hours` 48, `warning_threshold` 3, `repair_due_hours_urgent/normal/low` 24/72/168). Hibernate runs with `ddl-auto=validate`.
- Security: JSON session login `POST /api/auth/login`, `POST /api/auth/logout` (204), `GET /api/me`; CSRF token in the `XSRF-TOKEN` cookie echoed back in `X-XSRF-TOKEN`; 401/403 as `application/problem+json`; `@EnableMethodSecurity` with `@PreAuthorize("hasRole('ADMIN')")` on `SettingsController` (`GET /api/settings`, `PUT /api/settings/{key}`), the admin surface of this phase.
- `SettingsService` (typed getters, validated update), `AuditService` (Propagation.MANDATORY, current user id, Clock), `DomainException` hierarchy (409 / 422 / 404) and `ApiExceptionHandler` (one advice: 400 with field errors, 401, 403, 404, 409, 422, 500 without internals).
- `Clock` bean (Asia/Ho_Chi_Minh) and `MutableClock` test bean; `AbstractIntegrationTest` with one Testcontainers MySQL 8.4 per JVM, `DatabaseCleaner` (truncate + re-seed settings before each test), `ApiClient` (real HTTP with cookie jar and CSRF header), `TestUsers`.
- `DemoDataService` + `DemoDataSeeder` (demo profile): `admin`, `student`, `technician`, `accountant`, `affairs`, password `password`, Vietnamese names.
- Frontend: login page, `AuthProvider` on the `me` query, `RequireAuth`, `RequireRole`, sidebar filtered by role with a placeholder page per later-phase screen, `apiFetch` with CSRF cookie handling and ProblemDetail errors.

**BR tests passing:** none belong to Phase 0 (BR-01 to BR-11 start in Phase 2). Suite: 38 tests green with `./mvnw test` on Testcontainers MySQL 8.4: AUTH (AuthFlowTest, 10), CSRF (3), ROLE (RoleAccessTest, 6), SETTINGS (5), AUDIT (4), ERROR (ApiExceptionHandlerTest, 5), DEMO (3), INFRA (DmsApplicationTest, MutableClockTest). Frontend: `npm run build` and `npm run lint` pass.

**Done-when check:** with profiles `dev,demo`, each demo account logs in through the API and `/api/me` returns its role; `GET /api/settings` is 200 for ADMIN and 403 problem+json for STUDENT, TECHNICIAN, ACCOUNTANT and AFFAIRS; a login without the CSRF header is 403; logout returns 204 and the session is gone. The frontend shows only the menu of the signed-in role (`NAV_ITEMS` in `frontend/src/layout/navigation.ts`).

**Decisions**
- Spring Boot 3.5.16 is the last 3.x release (OSS support ended June 2026) and start.spring.io only offers 4.x, so the pom is hand-written. Moving to 4.x is a team decision.
- Enum fields carry `@JdbcTypeCode(SqlTypes.VARCHAR)` on VARCHAR columns (rule added to CLAUDE.md): Hibernate 6.6 otherwise expects a native MySQL ENUM column and `ddl-auto=validate` fails at startup.
- Login by username. `users` holds login identity only (username, password hash, full name, nullable email, role, enabled). Student data goes into a `students` table referencing `users.id` in Phase 2.
- Settings columns are `setting_key` / `setting_value` because `key` is reserved in MySQL. Only keys with defaults in CLAUDE.md are seeded; prices, rent and due days come with Phase 4.
- Clock zone Asia/Ho_Chi_Minh; instants stored as UTC `DATETIME(6)` (`hibernate.jdbc.time_zone=UTC`), dates as `DATE`.
- The login endpoint invokes a `SessionAuthenticationStrategy` (session id change + CSRF token rotation), so the SPA re-reads the cookie on every request.
- Demo accounts are created by the demo seeder, not Flyway: a database started without the demo profile has no accounts.
- Integration tests are not `@Transactional` (requests run on server threads; later locking tests need committed data). Test classes are named `*Test` so `./mvnw test` runs everything.
- Password hashes use the delegating encoder (`{bcrypt}` prefix). No Lombok; DTOs are records.
- Build machine: no JDK was installed, so Temurin 21.0.12.1 was extracted to `C:\Users\Admin\.jdks\jdk-21.0.12.1+1` (user-local, no PATH change). Set `JAVA_HOME` to it or install a JDK; Docker Desktop must be running for `./mvnw test`.

**Open questions**
1. Stay on Spring Boot 3.5.16 (no more OSS patches) or move to 4.x and update CLAUDE.md?
2. Meter readings: BUILD_PLAN Phase 4 gives them to ADMIN, the WORKFLOW design prompt to ACCOUNTANT. Only the menu placeholder (under ADMIN) is affected until Phase 4.
3. Should `debt_block_days` (BR-09, 60 days) be a setting or a constant?
4. Electricity and water prices, rent and invoice due days to seed in Phase 4.
5. A Settings admin page (the API exists) is a Phase 6 placeholder for now; move it earlier if useful.
6. The branch is named `phase-N`; rename before the PR (`git branch -m phase-N phase-0`). After `git add`, run `git update-index --chmod=+x backend/mvnw` so teammates on Linux or macOS can run the wrapper.

---

## Spring Boot 4.1.1 upgrade (2026-09-27)

**Built**
- Backend moved from Spring Boot 3.5.16 to 4.1.1 (latest 4.1.x): Spring Framework 7.0.9, Spring Security 7.1.1, Hibernate 7.4.5, Jackson 3.1.5, Flyway 12.4.0, Testcontainers 2.0.5, JUnit Jupiter 6.0.3, Tomcat 11.0.24. No features. Frontend unchanged.
- `pom.xml`: parent 4.1.1; `spring-boot-starter-web` -> `spring-boot-starter-webmvc`; `flyway-core` -> `spring-boot-starter-flyway` (plus `flyway-mysql`); one `-test` companion per main starter (webmvc, data-jpa, security, validation, flyway) instead of `spring-boot-starter-test` + `spring-security-test`; Testcontainers artifacts renamed to `testcontainers-mysql` and `testcontainers-junit-jupiter`. No classic starters, no `spring-boot-jackson2`.
- Main code: `ProblemDetailWriter` injects the Jackson 3 `JsonMapper`; the two 422 exceptions use `HttpStatus.UNPROCESSABLE_CONTENT` (old constant deprecated); `application.yml` moves `server.error.*` to `spring.web.error.*` (Boot 4 no longer binds the old keys).
- Test code: `ApiClient` on Jackson 3 (`JsonMapper.shared()`, unchecked `JacksonException`), `AbstractIntegrationTest` on `org.testcontainers.mysql.MySQLContainer`. Tests changed:
  - `findValuesAsText` -> `findValuesAsString` (removed in Jackson 3): ApiExceptionHandlerTest "ERROR @Valid failure maps to 400 with field errors", RoleAccessTest "ROLE ADMIN gets 200 on GET /api/settings and PUT /api/settings/offer_hours changes the value".
  - Expected constant `UNPROCESSABLE_CONTENT` (enum identity, both are 422): SettingsServiceTest "SETTINGS update() with a value that does not parse for the key type throws InvalidSettingValueException (422)".
  - `asText()` -> `asString()` (deprecated alias, 22 calls, same results): AuthFlowTest, CsrfProtectionTest, RoleAccessTest, ApiExceptionHandlerTest, DemoDataServiceTest.
  - No test disabled, deleted or weakened.
- `CLAUDE.md`: Backend and Tests stack lines; enum convention reworded.

**BR tests passing:** none yet (BR-01 to BR-11 start in Phase 2). Suite: 38/38 green with `./mvnw test` on Testcontainers MySQL 8.4. The build prints no warnings with deprecation output on, and the test log has no WARN lines.

**Verification**
- Dependency tree: no `com.fasterxml.jackson.core:jackson-databind`; only `jackson-annotations` keeps the old group id, as the migration guide says.
- `spring-boot-properties-migrator` run once with `dev,demo`: nothing to report; removed again.
- Started against the Phase 0 dev database: Flyway 12 validated the history written by Flyway 11 ("Schema `dms` is up to date"), Hibernate validation passed.
- curl walk-through of the Phase 0 done-when checks, and all five demo accounts logged in through the Vite proxy: same results as Phase 0 (ADMIN 200 on `/api/settings`, every other role 403). The sidebar was not re-checked in a browser; it is derived from `role`, which is unchanged.
- Hibernate 7 still stores `Instant` as UTC: `settings.updated_at` written through the API was 0 s from `UTC_TIMESTAMP()`.

**Decisions**
- Upgrade to 4.1.1 answers Phase 0 open question 1.
- Jackson 3 defaults adopted as is (`spring.jackson.use-jackson2-defaults` not set). Checked against our DTOs: unknown properties still ignored, dates still ISO strings, records keep declaration order, enum values unchanged (`Role` does not override `toString`). The new failures on null primitives and trailing tokens affect no current request DTO.
- Framework-imposed problem-body differences, accepted by the team: the 422 title is now "Unprocessable Content" (RFC 9110 wording); `"type":"about:blank"` is no longer written because Framework 7 made `ProblemDetail.type` null by default (RFC 9457 treats an absent type as about:blank); members come out in alphabetical order. Status, detail, instance, the other titles and custom members (`rule`, `errors`) are unchanged. No test or frontend code depends on the changed parts.
- Spring Security: no code change. Password logins now also carry a `FACTOR_PASSWORD` authority (Security 7 multi-factor support); role checks and `/api/me` are unaffected. The explicit `SpaCsrfTokenRequestHandler` stays instead of the new `csrf.spa()`, because the login strategy must share the same CSRF repository and handler beans.
- Enum convention: the Phase 0 rationale was wrong. Hibernate 6.6 and 7.4 both treat ENUM and VARCHAR as equivalent in schema validation (HHH-17908) and bind enums with `setString`. Proven by running all 38 tests with both `@JdbcTypeCode` annotations removed (green), then restoring them. The team kept the annotations for consistency with the Flyway DDL; CLAUDE.md now states the accurate reason.
- `scoring_config` (Phase 2): Hibernate 7.4 maps JSON columns with its own Jackson 3 mapper (it would prefer Jackson 2 if that were on the classpath), not Boot's `JsonMapper`, so `spring.jackson.*` settings do not apply to those columns.
- For later phases: `@SpringBootTest` no longer sets up MockMvc (add `@AutoConfigureMockMvc`), `@MockBean`/`@SpyBean` are gone (use `@MockitoBean`/`@MockitoSpyBean`), and Hibernate 7.4 dropped MySQL's implicit `max_fetch_depth=2`.

**Open questions:** none.
