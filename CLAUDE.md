# Dormitory Management System (DMS)

University group project (System Analysis and Design, HUST, Group 7). A web system for the HUST dormitory that models the current registration process faithfully — priority windows, self-service room choice, 30-minute payment holds, semester prepayment — and extends it into the residence lifecycle that is currently handled on paper and in Zalo groups: check-in and residence declaration, monthly electricity sharing, repairs, announcements and warnings.

The build is split into phases in `docs/BUILD_PLAN.md`. **Work on one phase per session.** Read the phase before starting, and read `docs/PROGRESS.md` to see what is already done.

## Stack

- **Backend:** Java 21, Spring Boot 4.1 (Spring Framework 7, Spring Security 7, Hibernate 7, Jackson 3), Maven. Spring Web MVC, Spring Data JPA (Hibernate), Spring Security, Bean Validation, Flyway. Modular starters: every main starter has its `-test` companion; no classic starters and no Jackson 2.
- **Database:** MySQL 8
- **Tests:** JUnit 6 (Jupiter), Spring Boot Test, Testcontainers 2 (MySQL). No H2 - locking behaviour must be tested on real MySQL
- **Frontend:** React + TypeScript (Vite), React Router, TanStack Query, Tailwind CSS
- **Language:** English everywhere — UI text, code, comments, commit messages. Seed data uses realistic Vietnamese student names.

## Repository layout

```
backend/    Spring Boot app, package vn.edu.hust.dms
frontend/   Vite React app
docs/       BUILD_PLAN.md, PROGRESS.md, WORKFLOW.md, plans/
docker-compose.yml   MySQL for local dev
```

Backend packages by feature, each with `entity`, `repository`, `service`, `web` (controller + DTOs):
`common` (security, audit, settings, errors, storage), `facility`, `registration`, `residence`, `billing`, `maintenance`, `communication`, `discipline`, `report`.

## Commands

```bash
docker compose up -d                                   # MySQL
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo
cd backend && ./mvnw test                              # must pass before a phase is done (needs Docker)
cd frontend && npm install && npm run dev              # Vite proxies /api to :8080
```

The `demo` profile runs `DemoDataSeeder` on startup when the database is empty.

## Roles

`users.role` enum: `STUDENT`, `ADMIN` (Centre Administrator), `BUILDING_MANAGER`, `TECHNICIAN`, `ACCOUNTANT`. Session-based Spring Security with a JSON login endpoint; CSRF token via cookie for the SPA. Authorisation with `@PreAuthorize`; ownership checks (a student only sees their own data) and building scope (BR-14) in the service layer. The frontend hides menus by role, but the backend is the only real enforcement.

Building managers are linked to buildings through `building_managers (user_id, building_id)`. A manager may have more than one building.

## Domain model

```
buildings < floors < rooms < beds
floors.gender_preference: null | MALE | FEMALE      (soft default only, BR-03)
room_types (building_id, name, capacity, area_m2, has_air_conditioning,
            has_water_heater, bathrooms, monthly_rent)
rooms (floor_id, room_type_id, code, gender)
rooms.gender: null | MALE | FEMALE                   (null = empty, unlocked)
room_assets (room_id, name, status)
building_managers (user_id, building_id)

student_profiles (user_id, student_code, gender, date_of_birth, citizen_id,
                  permanent_address, phone, family_contact, priority_group,
                  proof_status, photo_path, proof_path)
priority_group: UT1 | UT2 | UT3
proof_status: NOT_REQUIRED | PENDING | VERIFIED | REJECTED

registration_rounds (name, term_code, type, opens_at, closes_at,
                     stay_start, stay_end, hold_minutes)
registration_rounds.type: FIRST_TIME | STAY_ON | SUMMER
round_windows (round_id, priority_group, opens_at)
round_buildings (round_id, building_id)
registrations (student_id, round_id, bed_id, status, held_until, confirmed_at)
registrations.status: HELD | CONFIRMED | CHECKED_IN | EXPIRED | CANCELLED

residences (registration_id, student_id, bed_id, starts_on, ends_on, status)
residences.status: PENDING_CHECK_IN | ACTIVE | ENDED | RENEWED
residence_histories (residence_id, bed_id, from_date, to_date nullable)
                     (from_date and to_date are both inclusive)

meter_readings (room_id, month, reading_start, reading_end,
                override_reason, approved_by)
invoices (student_id, type, period, status, amount, issued_at, due_at)
         <  invoice_lines (kind, description, quantity, unit_price, amount)
invoices.type: SEMESTER_FEE | ELECTRICITY
invoices.status: ISSUED | PAID | OVERDUE | VOID
payments (invoice_id, amount, method, reference, paid_at)

repair_tickets (room_id, reported_by, technician_id, priority, status, due_at, escalated_at)
repair_tickets.status: NEW | ACKNOWLEDGED | IN_PROGRESS | RESOLVED | ESCALATED | CONFIRMED | CLOSED
announcements (building_id, title, body, published_by, published_at)
violations (student_id, term_code, description, recorded_by, created_at)
warning_reviews (student_id, term_code, status, reviewed_by)

settings (key, value)          (fees, prices, deadlines, thresholds)
audit_logs (subject_type, subject_id, from_status, to_status, user_id, created_at)
```

Schema changes only through Flyway migrations (`V{n}__description.sql`). Never use `ddl-auto=update`.

Money is `long` in VND. Never use `double` or `float` for money.

Uploaded files (photos, priority proof) are stored under the configured `app.storage-dir`, never in the database, and served only through an authorised endpoint — never as static files.

### Settings and their defaults

| Key | Default | Source |
|---|---|---|
| `water_fee_monthly` | 40000 | HUST notice for K71, semester 20261 |
| `equipment_fee` | 300000 | HUST notice for K71, semester 20261 |
| `electricity_price_per_kwh` | 3000 | resident interview |
| `default_hold_minutes` | 30 | HUST notice for K71, semester 20261 |
| `overdue_days_block_stay_on` | 30 | team decision |
| `repair_deadline_hours_urgent / normal / low` | 24 / 72 / 168 | team decision |
| `warning_threshold` | 3 | team decision |

Seed room types with the official monthly rents for B6 and B9: 6-student room 1,050,000; 8-student room 730,000; 10-student room 550,000 (all with air conditioning, water heater, one bathroom). Other buildings (B3, B5, B8, B10, B13) use the same structure with their own capacities and amenities; their rents are unknown, so seed plausible values and mark them in the seeder as estimates.

## Business rules

These are the core of the project. Every rule must have at least one test whose `@DisplayName` starts with its ID, e.g. `@DisplayName("BR-03 rejects holding a bed in a female room for a male student")`.

- **BR-01 Priority windows.** Each priority group has its own `round_windows.opens_at`. A student can hold a bed only when `now` is at or after their group's opening time and before the round's `closes_at`.
- **BR-02 One place per student.** At most one `HELD` or `CONFIRMED` registration per student per round, and at most one `ACTIVE` residence per student at any time. Enforced in the service layer inside a transaction, locking the student row.
- **BR-03 No mixed-gender rooms.** Holding a bed in an empty room sets `rooms.gender`. When the room has no held, confirmed or active occupants left, reset it to null. `floors.gender_preference` only sets the default filter in the room browser and never blocks a choice.
- **BR-04 Exclusive bed hold.** A bed can have at most one `HELD`, `CONFIRMED` or `CHECKED_IN` registration or `ACTIVE` residence. `@Transactional` service method loading the bed with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Two tests: two threads holding the same bed (exactly one succeeds), and 50 threads competing for 10 beds (exactly 10 holds, no bed held twice).
- **BR-05 Hold expiry.** `held_until = hold time + round.hold_minutes`. A scheduled job expires overdue holds: registration → `EXPIRED`, its semester fee invoice → `VOID`, bed freed, room gender re-evaluated (BR-03). A payment for an expired hold is rejected with 409.
- **BR-06 Meter validation.** `reading_end ≥ reading_start`, and `reading_start` equals the previous month's `reading_end`, unless `override_reason` and `approved_by` are both set.
- **BR-07 Electricity share by days.** For each room and month: room cost = (reading_end − reading_start) × `electricity_price_per_kwh`. Each occupant's share = room cost × (their days in that room that month) ÷ (total occupant-days), using `residence_histories` with inclusive dates. Round each share down to whole VND; give the remainder to the occupant with the most days (ties: lowest residence id). Shares must sum exactly to room cost. One `ELECTRICITY` invoice per occupant.
- **BR-08 Immutable invoices.** Once issued, an invoice and its lines are never updated or deleted. The only allowed changes are status transitions to `PAID`, `OVERDUE` or `VOID`. Corrections go on a later invoice.
- **BR-09 Debt blocks stay-on.** A student with an `ELECTRICITY` invoice unpaid more than `overdue_days_block_stay_on` days after `issued_at` cannot hold a bed in a `STAY_ON` round.
- **BR-10 Repair deadline.** `due_at` from priority via the repair deadline settings. A scheduled job marks open tickets past `due_at` as `ESCALATED` and notifies the building manager.
- **BR-11 Warning threshold.** When a student's violations in one term reach `warning_threshold`, create a `warning_reviews` row for the Centre Administrator. Nothing else happens automatically.
- **BR-12 Semester fee.** Months = number of calendar months touched by `[stay_start, stay_end]`. Fee = (room type `monthly_rent` + `water_fee_monthly`) × months, plus `equipment_fee` only if the round type is `FIRST_TIME`. Issued as one `SEMESTER_FEE` invoice when the bed is held, due at `held_until`. Test vectors (stay 2026-09-03 to 2027-02-25 = 6 months, first time): 8-student room 4,920,000; 10-student room 3,840,000; 6-student room 6,840,000. A move-in on the last day of a month still counts that whole month.
- **BR-13 Stay-on keeps the bed.** In a `STAY_ON` round, a student with an `ACTIVE` residence can only hold their current bed, and those beds are not offered to anyone else during that round. When the stay-on round closes, beds whose residents did not confirm become available to later rounds.
- **BR-14 Building scope.** A `BUILDING_MANAGER` can read and change only data belonging to buildings linked to them in `building_managers`. Any other building returns 404, not 403, so building existence is not leaked.
- **BR-15 Proof before check-in.** A student whose `priority_group` is `UT1` or `UT2` cannot be checked in unless `proof_status = VERIFIED`. Only a building manager of that building (or ADMIN) can verify or reject.

## Code conventions

- State transitions live in service classes (e.g. `RegistrationService.hold(...)`), never in controllers or entities' setters. Each transition validates the current state, performs the change, and writes an `audit_logs` row in the same transaction.
- Every status column is a Java enum mapped with `@Enumerated(EnumType.STRING)`.
- Enum fields map to `VARCHAR` columns and carry `@JdbcTypeCode(SqlTypes.VARCHAR)`, so Hibernate's column type matches the Flyway DDL exactly. Schema validation would also pass without it (Hibernate treats ENUM and VARCHAR as equivalent, HHH-17908); keep it for consistency.
- Invalid transitions throw a `DomainException` subclass; one `@RestControllerAdvice` maps them to RFC 9457 `ProblemDetail` responses: 409 for state conflicts and concurrency (e.g. BR-04, BR-05 late payment), 422 for business rule violations (e.g. BR-01, BR-02, BR-03, BR-09, BR-15).
- Controllers take and return DTOs (Java records), never JPA entities.
- All time-based logic reads the current time from an injected `java.time.Clock` bean, never `LocalDateTime.now()` directly. Tests replace it with a mutable test clock to move time forward.
- Scheduled jobs (`@Scheduled`) only call service methods; the logic stays testable without the scheduler.
- Integration tests extend one shared `AbstractIntegrationTest` using a single reused Testcontainers MySQL instance.
- `DemoDataSeeder`: buildings B3, B5, B6, B8, B9, B10, B13 with their room types; one manager per building; ~300 students across UT1/UT2/UT3; one open first-time round for semester 20261.

## Working rules

- Do not implement anything listed as out of scope in `docs/BUILD_PLAN.md`.
- Write the business-rule tests for a phase first, then the implementation.
- Run `./mvnw test` before declaring a phase done. Do not skip, disable or delete failing tests to make the suite pass.
- At the end of each phase, append to `docs/PROGRESS.md`: what was built, which BR tests pass, and any decisions or open questions for the team.
- If a requirement is ambiguous, stop and ask rather than guessing — the team owns the specification.