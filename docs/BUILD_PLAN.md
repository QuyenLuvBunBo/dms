# DMS Build Plan

Seven phases. Each phase is one Claude Code session (or a few), ends with `./mvnw test` passing, and produces something the team can demo at sprint review.

Mapping to the proposal's sprints: phases 0–1 in Sprint 3, phases 2–3 in Sprint 4, phases 4–5 in Sprint 5, phase 6 in Sprint 6.

**Out of scope for every phase:** real payment gateway, native mobile app, integration with the university's existing systems, access control hardware, canteen, machine learning.

---

## Phase 0 — Project setup and roles

**Build**
- Monorepo with `backend/` (Spring Boot 3, Java 21, Maven) and `frontend/` (Vite + React + TypeScript + Tailwind)
- `docker-compose.yml` with MySQL 8; Flyway baseline migration
- Spring Security: session login via JSON endpoint, CSRF cookie, `/api/me` returning user and role
- `users` table with `role` enum; seeded one account per role
- `settings` table with a typed `SettingsService` and seeded defaults
- `audit_logs` table and an `AuditService` used by all state transitions
- `DomainException` hierarchy and `@RestControllerAdvice` returning `ProblemDetail`
- Injected `Clock` bean and a mutable test clock
- `AbstractIntegrationTest` with a reused Testcontainers MySQL
- Frontend: login page, role-based layout and navigation, API client with CSRF handling
- `docs/PROGRESS.md` created

**Done when**
- Each role can log in and sees only its own menu
- A student calling an admin endpoint gets 403

**Tests:** role access tests for every role against real endpoints.

---

## Phase 1 — Facility management

**Build**
- CRUD API and screens for buildings, floors, rooms, beds, room assets (ADMIN only)
- Room detail page: beds, current occupants (empty for now), gender, assets
- Seeder: 3 buildings, rooms of 4/6/8 beds

**Done when**
- Admin can create a room and its beds; bed count matches room capacity
- A room with occupants cannot be deleted

**Tests:** capacity validation, delete protection.

---

## Phase 2 — Admission rounds and applications

**Build**
- Admin opens an admission round: quota, deadline, `scoring_config` weights
- Student creates, edits and submits an application (DRAFT → SUBMITTED); editing locked after submit
- Priority score computed on submit (BR-01)
- Admin review screen: applications sorted by score; approve / reject / waitlist
- Student sees application status and score breakdown

**Done when**
- Changing the weights of a round changes the ranking without any code change
- Submissions after the deadline are rejected

**Tests:** BR-01 (score computed from config; two configs give different rankings), deadline enforcement using the test clock, invalid transitions (e.g. approving a draft returns 409).

---

## Phase 3 — Bed assignment, offers and contracts

**Build**
- Assign an approved applicant to a bed (BR-02, BR-03, BR-04)
- Suggested assignment: fill beds greedily by score, respecting gender
- Waitlist: when a bed is free, create a `bed_offer` to the top eligible waitlisted applicant; student accepts or declines
- Scheduled job expiring offers (BR-05)
- Contract generated on acceptance; check-in activates it and opens a `residence_histories` row
- Room transfer: closes the current history row, opens a new one, same contract
- Check-out: closes history, frees bed, resets room gender if empty, records deposit settlement

**Done when**
- The full flow works in the UI: apply → approve → assign → accept → check in → transfer → check out
- Room gender unlocks when the last occupant leaves

**Tests:** BR-02, BR-03 (lock and unlock), BR-04 (two threads assigning the same bed — exactly one succeeds), BR-05 (advance the test clock past expiry; applicant moves to end of waitlist; next applicant gets the offer), transfer creates correct history rows.

---

## Phase 4 — Utility readings and billing

**Build**
- Admin records monthly readings per room (BR-06)
- Accountant generates invoices for a month: rent line + utility share line (BR-07) + any adjustment lines
- Invoice issue (BR-08); simulated payment page with a "Pay" button that posts a fake gateway callback; accountant can also record a manual payment
- Scheduled job marking invoices overdue and sending reminders
- Student invoice page shows the utility calculation: room total, their days, total occupant-days, their share
- Renewal flow with the debt check (BR-09)

**Done when**
- A student who transferred mid-month gets two utility lines, one per room, each with the correct days
- Shares in every room sum exactly to the room cost

**Tests:** BR-06 (lower reading rejected; accepted with override + approver), BR-07 as plain unit tests on the allocation class (full month; mid-month move-in; mid-month transfer; rounding remainder goes to the right person; shares sum to total), BR-08 (updating an issued invoice fails), BR-09.

---

## Phase 5 — Maintenance and discipline

**Build**
- Student reports a fault for their room with priority and description
- Admin assigns a technician; technician moves the ticket through its states; student confirms resolution
- Scheduled job escalating overdue tickets (BR-10)
- Admin records violations; threshold creates eviction proposal; AFFAIRS approves or rejects (BR-11); approval terminates the contract through the normal check-out path

**Done when**
- A technician sees only tickets assigned to them
- An approved eviction frees the bed and closes residence history

**Tests:** BR-10 (due date by priority; escalation after advancing the test clock), BR-11 (threshold creates proposal; nothing changes until approval), ticket state transitions.

---

## Phase 6 — Reports, notifications and hardening

**Build**
- Dashboards: occupancy by building and room type, outstanding debt, consumption trend by month, average repair resolution time
- In-app notifications (stored in a `notifications` table, shown in a bell menu) plus email via a logging mail sender: application result, bed offer, invoice issued, overdue reminder, ticket status
- Audit log viewer for admin, filterable by subject
- Mobile layout pass on student pages
- Full seed of a realistic semester for the demo

**Done when**
- Starting with the `demo` profile on an empty database produces data where every dashboard shows meaningful figures
- Every BR-01 to BR-11 test passes

**Tests:** report figures checked against seeded data, one end-to-end integration test of a whole semester for one student.

---

## How to run each phase

Every phase follows the same loop — branch, `/clear`, `/plan`, review the plan, implement, verify, close out and open a PR. The exact prompts and the per-phase review checklist are in `docs/WORKFLOW.md`.
