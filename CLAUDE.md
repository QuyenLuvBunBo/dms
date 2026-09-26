# Dormitory Management System (DMS)

University group project (System Analysis and Design, HUST, Group 7). A web system covering the full dormitory residence lifecycle: admission → bed assignment → contract → monthly billing → maintenance and discipline → renewal or check-out.

The build is split into phases in `docs/BUILD_PLAN.md`. **Work on one phase per session.** Read the phase before starting, and read `docs/PROGRESS.md` to see what is already done.

## Stack

- **Backend:** Java 21, Spring Boot 3, Maven. Spring Web, Spring Data JPA (Hibernate), Spring Security, Bean Validation, Flyway
- **Database:** MySQL 8
- **Tests:** JUnit 5, Spring Boot Test, Testcontainers (MySQL). No H2 — locking behaviour must be tested on real MySQL
- **Frontend:** React + TypeScript (Vite), React Router, TanStack Query, Tailwind CSS
- **Language:** English everywhere — UI text, code, comments, commit messages. Seed data uses realistic Vietnamese student names.

## Repository layout

```
backend/    Spring Boot app, package vn.edu.hust.dms
frontend/   Vite React app
docs/       BUILD_PLAN.md, PROGRESS.md
docker-compose.yml   MySQL for local dev
```

Backend packages by feature, each with `entity`, `repository`, `service`, `web` (controller + DTOs):
`common` (security, audit, settings, errors), `facility`, `admission`, `contract`, `billing`, `maintenance`, `discipline`, `report`.

## Commands

```bash
docker compose up -d                                   # MySQL
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo
cd backend && ./mvnw test                              # must pass before a phase is done (needs Docker)
cd frontend && npm install && npm run dev              # Vite proxies /api to :8080
```

The `demo` profile runs `DemoDataSeeder` on startup when the database is empty.

## Roles

`users.role` enum: `STUDENT`, `ADMIN`, `TECHNICIAN`, `ACCOUNTANT`, `AFFAIRS`. Session-based Spring Security with a JSON login endpoint; CSRF token via cookie for the SPA. Authorisation with `@PreAuthorize` on service or controller methods; ownership checks (a student only sees their own data) in the service layer. The frontend hides menus by role, but the backend is the only real enforcement.

## Domain model

```
buildings < floors < rooms < beds
rooms.gender: null | MALE | FEMALE              (null = empty, unlocked)
admission_rounds < applications > students
admission_rounds.scoring_config: JSON           (criteria weights, see BR-01)
applications.status: DRAFT | SUBMITTED | APPROVED | REJECTED | WAITLISTED | OFFERED | ASSIGNED | EXPIRED
bed_offers (application_id, bed_id, expires_at, status)
contracts (student_id, bed_id, starts_on, ends_on, status, deposit)
contracts.status: PENDING | ACTIVE | RENEWED | TERMINATED | EXPIRED | SETTLED
residence_histories (contract_id, bed_id, from_date, to_date nullable)
meter_readings (room_id, month, electricity_start, electricity_end, water_start, water_end, override_reason, approved_by)
invoices (student_id, month, status, issued_at)  <  invoice_lines (type, description, amount)
invoices.status: DRAFT | ISSUED | PAID | OVERDUE
payments (invoice_id, amount, method, reference, paid_at)
repair_tickets (room_id, reported_by, technician_id, priority, status, due_at, escalated_at)
repair_tickets.status: NEW | ACKNOWLEDGED | IN_PROGRESS | RESOLVED | ESCALATED | CONFIRMED | CLOSED
violations (student_id, term, description, recorded_by)
eviction_proposals (student_id, status, approved_by)
settings (key, value)                           (prices, deadlines, thresholds)
audit_logs (subject_type, subject_id, from_status, to_status, user_id, created_at)
```

Schema changes only through Flyway migrations (`V{n}__description.sql`). Never use `ddl-auto=update`.

Money is `long` in VND. Never use `double` or `float` for money.

## Business rules

These are the core of the project. Every rule must have at least one test whose `@DisplayName` starts with its ID, e.g. `@DisplayName("BR-03 rejects assigning a male student to a female room")`.

- **BR-01 Configurable priority scoring.** Score = Σ (weight × normalised criterion value). Weights come from `admission_rounds.scoring_config`, never from code. Criteria: policy category, distance from home, GPA, year of study.
- **BR-02 One active contract per student.** Enforced in the service layer inside a transaction, locking the student row, since MySQL has no partial unique index.
- **BR-03 No mixed-gender rooms.** Assigning to an empty room sets `rooms.gender`. When the last occupant leaves, reset it to null.
- **BR-04 Exclusive bed assignment.** `@Transactional` service method loading the bed with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Write a test that runs two assignments of the same bed on two threads (ExecutorService + CountDownLatch) and proves only one succeeds.
- **BR-05 Offer expiry.** Bed offers expire after the `offer_hours` setting (default 48). An expired offer returns the application to the end of the waitlist and the bed is offered to the next eligible applicant.
- **BR-06 Meter validation.** End reading ≥ start reading, and start reading = previous month's end reading, unless `override_reason` and `approved_by` are both set.
- **BR-07 Utility allocation by days.** For each room and month: room cost = electricity units × price + water units × price. Each occupant's share = room cost × (their days in that room that month) ÷ (total occupant-days). Days come from `residence_histories`. Round each share down to whole VND; give the remainder to the occupant with the most days (ties: lowest contract id). Shares must sum exactly to room cost.
- **BR-08 Immutable invoices.** Once `ISSUED`, an invoice and its lines are never updated or deleted. Corrections go into an `ADJUSTMENT` line on the next invoice.
- **BR-09 Debt blocks renewal.** A student with any invoice unpaid more than 60 days after issue cannot renew.
- **BR-10 Repair deadline.** `due_at` from priority: urgent 24h, normal 72h, low 7 days (in settings). A scheduled job escalates open tickets past `due_at`.
- **BR-11 Warning threshold.** Reaching the `warning_threshold` setting (default 3) violations in one term creates an eviction proposal. It takes effect only after approval by an `AFFAIRS` user.

## Code conventions

- State transitions live in service classes (e.g. `ApplicationService.approve(id)`), never in controllers or entities' setters. Each transition validates the current state, performs the change, and writes an `audit_logs` row in the same transaction.
- Every status column is a Java enum mapped with `@Enumerated(EnumType.STRING)`.
- Invalid transitions throw a `DomainException` subclass; one `@RestControllerAdvice` maps them to RFC 7807 `ProblemDetail` responses (409 for state conflicts, 422 for rule violations).
- Controllers take and return DTOs (Java records), never JPA entities.
- All time-based logic reads the current time from an injected `java.time.Clock` bean, never `LocalDateTime.now()` directly. Tests replace it with a mutable test clock to move time forward.
- Scheduled jobs (`@Scheduled`) only call service methods; the logic stays testable without the scheduler.
- Integration tests extend one shared `AbstractIntegrationTest` using a single reused Testcontainers MySQL instance.
- `DemoDataSeeder`: 3 buildings, rooms of 4, 6 and 8 beds, ~200 students, one open admission round.

## Working rules

- Do not implement anything listed as out of scope in `docs/BUILD_PLAN.md`.
- Write the business-rule tests for a phase first, then the implementation.
- Run `./mvnw test` before declaring a phase done. Do not skip, disable or delete failing tests to make the suite pass.
- At the end of each phase, append to `docs/PROGRESS.md`: what was built, which BR tests pass, and any decisions or open questions for the team.
- If a requirement is ambiguous, stop and ask rather than guessing — the team owns the specification.
