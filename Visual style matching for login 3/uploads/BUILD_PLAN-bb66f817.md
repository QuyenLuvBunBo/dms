# DMS Build Plan

Seven phases. Each phase is one Claude Code session (or a few), ends with `./mvnw test` passing, and produces something the team can demo at sprint review.

Mapping to the proposal's sprints: phases 0–1 in Sprint 3, phase 2 in Sprint 4, phases 3–5 in Sprint 5, phase 6 in Sprint 6.

**Out of scope for every phase:** real payment gateway, sign-in with university email and OTP, integration with the existing registration portal or student information system, native mobile app, access control hardware, canteen, machine learning.

---

## Phase 0 — Project setup and roles ✅ done

Also done: upgrade to Spring Boot 4.1 (see PROGRESS.md).

---

## Phase 1 — Facilities, room types and building managers

**Build**
- Role change: add `BUILDING_MANAGER`, remove `AFFAIRS` (migration + seed update; one demo manager account per building)
- Replace the Phase 0 seeded settings with the keys and defaults in CLAUDE.md
- CRUD API and screens (ADMIN) for buildings, floors with optional gender preference, room types, rooms, beds, room assets
- `building_managers` link and a screen to assign managers to buildings
- Building manager sees only their buildings (BR-14)
- Seed B3, B5, B6, B8, B9, B10, B13 with room types; B6 and B9 use the official rents

**Done when**
- Admin can create a room type and a room; bed count equals room type capacity
- A room with occupants cannot be deleted
- The B6 manager cannot see or change anything in B9 (404)

**Tests:** capacity, delete protection, BR-14 on every facility endpoint.

---

## Phase 2 — Registration rounds, declaration, room choice and holds

**Build**
- ADMIN opens a round: name, term code, type, `opens_at`/`closes_at`, stay period, hold minutes, one opening time per priority group, buildings offered
- Student declaration: personal, academic, address and family details, priority group, photo upload, proof upload (required for UT1/UT2; sets `proof_status = PENDING`)
- Student sees rounds with their own group's opening time and a countdown
- Registration wizard in the same step order as the current portal: confirm details → choose building → choose room (free beds, room type, rent, amenities, room gender, floor preference as default filter) → confirm
- Confirm creates a `HELD` registration and a `SEMESTER_FEE` invoice (BR-12), and shows a hold countdown
- Scheduled job expiring holds (BR-05)

**Done when**
- A UT3 student cannot hold a bed before the UT3 window opens
- Two browsers racing for the last free bed: one gets it, the other gets a clear 409 message
- An unpaid hold disappears after the hold time and the bed shows as free again

**Tests:** BR-01, BR-02, BR-03 (lock, unlock on expiry, floor preference never blocks), BR-04 (two threads; 50 threads for 10 beds), BR-05 (advance the test clock), BR-12 (all three test vectors and the last-day-of-month case), file upload access (a student cannot fetch another student's photo).

---

## Phase 3 — Payment, check-in, residence, stay-on and transfer

**Build**
- Simulated QR payment page; paying a `HELD` registration confirms it, creates a `PENDING_CHECK_IN` residence, and sends a confirmation notification
- Late payment on an expired hold is rejected (BR-05)
- Building manager check-in screen: search student, view photo and declaration, verify or reject priority proof (BR-15), check in → residence `ACTIVE`, registration `CHECKED_IN`, first `residence_histories` row
- Residence declaration export (CSV) per building for residents currently checked in
- `STAY_ON` round: residents with an `ACTIVE` residence confirm their current bed; semester fee without equipment fee (BR-12, BR-13); debt check (BR-09) using seeded overdue electricity invoices
- Room transfer within a semester (proposed improvement — process not yet confirmed with the dormitory office): closes the current history row, opens a new one, same residence, BR-03 and BR-04 still apply
- End-of-term job: residences not renewed become `ENDED`

**Done when**
- The full flow works in the UI: declare → hold → pay → check in → transfer → stay on
- A UT1 student with unverified proof cannot be checked in

**Tests:** BR-05 late payment, BR-09, BR-13 (stay-on bed not offered to others; released after round closes), BR-15, transfer history rows, CSV export respects BR-14.

---

## Phase 4 — Electricity billing

**Build**
- Building manager records end-of-month meter readings per room (BR-06), with a grid for the whole building
- Generate `ELECTRICITY` share invoices for a month (BR-07)
- Student electricity page: room consumption, price per kWh, their days, total occupant-days, their share
- Scheduled job marking invoices overdue and sending reminders
- Simulated payment for electricity invoices

**Done when**
- A student who transferred mid-month gets two electricity invoices, one per room, each with the correct days
- Shares in every room sum exactly to the room cost

**Tests:** BR-06 (lower reading rejected; accepted with override and approver), BR-07 as plain unit tests on the allocation class (full month; mid-month move-in; mid-month transfer; rounding remainder to the right person; sum equals total), BR-08 (issued invoice cannot be updated; only status transitions allowed).

---

## Phase 5 — Maintenance, announcements and discipline

**Build**
- Student reports a fault for their room; building manager assigns a technician; technician updates status; student confirms
- Scheduled escalation of overdue tickets (BR-10)
- Building announcements: manager publishes, residents of that building see them in a feed and get a notification
- Violations recorded by the building manager; threshold creates a warning review for ADMIN (BR-11)

**Done when**
- A technician sees only tickets assigned to them
- A resident of B6 never sees B9 announcements

**Tests:** BR-10, BR-11, BR-14 on tickets, announcements and violations, ticket state transitions.

---

## Phase 6 — Reports, notifications and hardening

**Build**
- Dashboards: occupancy by building and room type; registration results per priority window (held, confirmed, expired); unpaid electricity by building; repair resolution time
- In-app notifications (stored, shown in a bell menu) plus email via a logging mail sender
- Audit log viewer for ADMIN, filterable by subject
- Mobile layout pass on student pages
- A load test of a window opening: a script that has several hundred simulated students hold beds at the same moment, and checks that no bed is held twice and every hold is either confirmed or expired afterwards
- Full demo seed of a realistic semester

**Done when**
- Starting with the `demo` profile on an empty database gives meaningful figures on every dashboard
- Every BR-01 to BR-15 test passes

**Tests:** report figures checked against seeded data, one end-to-end integration test of a whole semester for one student.

---

## How to run each phase

Every phase follows the same loop — branch, `/clear`, `/plan`, review the plan, implement, verify, close out and open a PR. The exact prompts and the per-phase review checklist are in `docs/WORKFLOW.md`.