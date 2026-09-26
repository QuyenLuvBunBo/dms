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
