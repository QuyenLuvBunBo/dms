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

---

## Phase 1 - Facilities, room types and building managers (2026-09-28)

**Built**
- Spec v2 cleanup: `V3__spec_v2_roles_and_settings.sql` turns AFFAIRS accounts into BUILDING_MANAGER (kept, not deleted, because of the audit FK) and replaces the Phase 0 settings with the nine CLAUDE.md keys and defaults. `Role` has BUILDING_MANAGER instead of AFFAIRS; `SettingKey` has the new keys and `SettingType` the new types MONEY, MINUTES and DAYS (`getDuration` works for minutes, hours and days); `AuditSubjectType` lists the Spec v2 aggregates (REGISTRATION, RESIDENCE, INVOICE, REPAIR_TICKET, WARNING_REVIEW, ROOM_ASSET, USER); the placeholder menus follow the Spec v2 screens of every role.
- Facility module (`vn.edu.hust.dms.facility`): `V4__facilities.sql` (buildings, floors, room_types, rooms, beds, room_assets, building_managers; foreign keys without cascade), entities, repositories, services and controllers for buildings, floors with an optional gender preference, room types, rooms, beds, room assets and manager links: 27 endpoints, 24 of them building-scoped, plus `GET/POST /api/buildings` and `GET /api/building-managers`.
- BR-14: `BuildingScope` (ADMIN sees every building, a BUILDING_MANAGER the buildings linked to them, read on every request, any other role none) and `FacilityLookup`, the one place the facility services load data through, so a missing id and an id in another building get the same 404 detail. For a manager the scope check runs before the ADMIN-only check: another building is always 404, and 403 only appears for an ADMIN-only action inside their own building.
- Beds and occupancy: a new room gets exactly `capacity` beds coded 1..N; ADMIN may remove a free bed and add beds back up to the capacity (lowest free code by default). The `BedOccupancy` interface has one production implementation in Phase 1, `EmptyBedOccupancy`, which reports no bed as occupied because no Phase 1 table can place a student in a bed. `OccupancyService` combines every implementation and guards both room deletion and single-bed removal (409).
- Errors: `ConflictException` (409) and `InvalidRequestException` (422 without a rule id); `DataIntegrityViolationException` maps to 409 without SQL details.
- Demo seed: `admin`, `student`, `technician`, `accountant` and one manager per building (`manager.b3` ... `manager.b13`); buildings B3, B5, B6, B8, B9, B10 and B13 from the new Buildings table in CLAUDE.md (resident data); official rents for B6 and B9 and team estimates elsewhere; floor gender preferences from the resident interview (FEMALE on floors 2 and 3, MALE on floors 1 and 4, none elsewhere); estimated layout of 5 floors x 6 rooms (B3 and B5: 4 floors) and 3 or 4 assets per room. That is 198 rooms and 1,656 beds; every estimate is marked ESTIMATE in `DemoFacilities`. The seed goes through the facility services as the seeded admin, so it obeys the same rules as the API.
- Frontend: Facilities (ADMIN) and My buildings (manager) list, building page with Rooms (by floor), Floors and Room types tabs, room page with beds and assets, Building managers screen (one checkbox per manager and building). ADMIN-only buttons are hidden from managers, and an API 404 shows the not-found page.
- Login redirect fix for a Phase 0 bug found in the Phase 1 UI check (see Decisions).
- Docs: CLAUDE.md has the Buildings table and the domain-model details decided in the plan (building code and name, floor number, bed code, asset statuses, room code unique per building); README (Spec v2 description, demo accounts, reset note); the approved plan in `docs/plans/phase-1.md`; the UI walkthrough screenshots in `docs/screens/phase-1/`.

**BR tests passing:** BR-14 (`BuildingScopeTest`: 9 test methods, 85 runs). For every one of the 24 building-scoped endpoints the B6 manager gets 404 on a B9 resource with nothing in B9 changed, and that 404 is identical to the one for a missing id. The manager reaches every allowed endpoint in B6, unassignment applies on the next request, and ADMIN is never refused by scope. BR-01 to BR-13 and BR-15 belong to later phases. Suite: 180/180 green with `./mvnw test` on Testcontainers MySQL 8.4 in 3.5 minutes: BR-14 85, ACCESS 21, CAPACITY 7, DELETE 6, FACILITY 9, MANAGERS 4, MIGRATION 1, DEMO 9, and the Phase 0 suites AUTH 10, CSRF 3, ROLE 6, SETTINGS 5, AUDIT 4, ERROR 8, INFRA 2. A mutation check (every manager allowed to see every building) made 50 BR-14 runs fail. Frontend: `npm run build` and `npm run lint` pass.

**Done-when check** (throwaway MySQL on port 3307, profiles `dev,demo`; the dev database was not touched)
- Flyway applied V1 to V4; the seed created 11 accounts and 7 buildings.
- API script, 22 checks passed: ADMIN created room type "Renovated 4-student room" (capacity 4) and room 507 in B6, which got beds 1 to 4; a fifth bed was refused (409) and a removed bed came back as code 1. manager.b6 saw only B6 and got 404 on GET of a B9 room (same detail as a missing room), on PUT of a B9 asset (asset unchanged) and on DELETE of a B9 room; 403 on DELETE of a B6 room; 200 on changing a B6 asset. STUDENT, TECHNICIAN and ACCOUNTANT got 403 on `/api/settings`, `/api/buildings` and a room.
- UI walkthrough in headless Chromium (Playwright): the same flows through the screens, including the duplicate room code error in the form and the not-found page for a B9 room URL as manager.b6. Screenshots in `docs/screens/phase-1/`.
- "A room with occupants cannot be deleted" and the single-bed case are proven by `FacilityDeleteProtectionTest` through `TestBedOccupancy`; in the running app no bed can be occupied before Phase 2.

**Decisions**
- Plan review (2026-09-28): managers read their own buildings and manage room assets there, everything else is ADMIN-only; the occupancy interface above; beds generated to capacity, then removed or added up to it; the old-model names (audit subject types, menus) replaced now.
- Plan ambiguities 1 to 13 decided by the team: every default accepted except item 9 (seed data), replaced with resident data (see Built). Among them: asset statuses GOOD, DAMAGED, MISSING, with creation and every status change audited as ROOM_ASSET; deletes bottom-up (a building without floors and room types, a floor without rooms, an unused room type, a room without occupants, which takes its beds and assets with it); a room type's capacity cannot drop below a room's bed count, and raising it adds no beds; old AFFAIRS accounts become BUILDING_MANAGER without a building; no user management yet; a building may have several managers; facility data is readable only by ADMIN and BUILDING_MANAGER.
- Phase 0 open questions 2 and 3 are answered by Spec v2: meter readings belong to the building manager (the placeholder moved there), and BR-09's day count is the setting `overdue_days_block_stay_on`.
- Phase 0 tests changed only where the removed setting keys forced it: `RoleAccessTest` and `CsrfProtectionTest` write to `default_hold_minutes` instead of `offer_hours` (expected type MINUTES, list size 9, unchanged value 30); `SettingsServiceTest` checks the nine new defaults and the new types; `DmsApplicationTest` expects V1 to V4; `DemoDataServiceTest` counts `DemoAccount` values instead of one account per role; `AuditServiceTest` and the error probes use Spec v2 names. No role assertion changed: STUDENT, TECHNICIAN and ACCOUNTANT still get 403 on the ADMIN endpoints.
- `DatabaseCleaner` restores a JVM-wide snapshot of the migrated settings instead of re-running `V2__seed_settings.sql`, which would bring the Phase 0 keys back.
- Locking: `addBed` locks the room and its room type, a capacity change locks the room type, `deleteRoom` locks the room and all its beds, and `removeBed` locks the bed. Each locking read is the first statement of its transaction, so the reads after it see what concurrent writers committed.
- Codes are compared case-insensitively (column collation) and listed in natural order (B6 before B10, bed 9 before bed 10). Room codes are unique per building: the service checks it, the database enforces it per floor.
- Small deviations from the plan's file list: one `RoomRequest` for create and update, `AddBedRequest` (optional code) and `BedRequest` (relabel), the helper `FacilityOrder`; `ApiClient` gained `delete` and `send`, `AbstractIntegrationTest` gained `loggedIn(...)` and `mysql()`.
- Login redirect fix (Phase 0 bug, fixed at the team's request): after Sign out, the login page sent the next user to the previous user's page, e.g. a student to a manager's room page, or a manager to "Access denied" after the admin signed out on `/building-managers`. The query cache also survived Sign out, so a manager signing in after the admin saw the admin's cached list of all 7 buildings for up to 30 seconds (the API itself was never affected). Now `RequireAuth` remembers the requested page together with who last had the tab (`auth/lastSession.ts`, kept in sessionStorage). The login page returns there only when nobody had signed in on this tab before (a link opened while signed out) or when the same user's session expired; after Sign out it is always the home page. Every cached API answer is dropped when a session ends. The frontend has no test runner, so the fix was verified with a Playwright script outside the repo: 8 checks passed. They cover admin signing out and manager.b6 landing on their home page and seeing only B6; manager.b6 signing out on a room page and the student landing on the student home page; the same after reloading the login page; an expired session returning the same user to their page but sending another user home; and a link opened while signed out still leading to its page. The same script failed where expected against the build from before the fix.

**Open questions**
1. Phase 2 must replace the Phase 1 BedOccupancy implementation with one based on registrations and residences, and add an integration test that deleting a room or removing a bed with a HELD registration fails.
2. BUILD_PLAN creates residences only in Phase 3 (plan ambiguity 13); the Phase 2 checklist in WORKFLOW.md says the same (registrations in Phase 2, residences in Phase 3). The real implementation should use a locking read (`SELECT ... FOR SHARE`): its callers already hold the bed locks but may have an older REPEATABLE READ snapshot (see the `BedOccupancy` Javadoc). The foreign keys to `beds` are the backstop.
3. Once Phase 2 adds registrations, rooms whose beds have history can no longer be hard-deleted: block or archive (plan ambiguity 4; the WORKFLOW checklist leans towards blocking).
4. No phase covers user management yet: building manager accounts exist only in the seed.
5. Seed values that are still estimates: rents outside B6 and B9, floors per building, rooms per floor, the split of a floor's rooms across room types, and the assets per room.
6. Databases created before Phase 1 get V3 and V4 but no demo buildings, because the seed only runs on an empty database. Reset with `docker compose down -v` (this deletes local data).
7. The seed takes about 5 seconds, so `DemoDataServiceTest` accounts for about 55 of the 210 seconds of the suite; its checks can be grouped if the suite gets too slow.
8. The frontend has no test runner, so this phase's Playwright checks are not in the repo. Adding one (for example Playwright tests in `frontend/`) is a team decision.
