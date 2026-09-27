# Phase 1 plan - Facilities, room types and building managers

## Context

Phase 0 (plus the Spring Boot 4.1 upgrade) is done. After the field study the spec was rewritten ("Spec v2"): the AFFAIRS role, scoring, offers, contracts and the Phase 0 settings keys are gone; CLAUDE.md now describes building managers, room types per building, and the settings table with new keys. Phase 1 of `docs/BUILD_PLAN.md` brings the code onto that model and builds the facility module that every later phase stands on:

- Role change: add `BUILDING_MANAGER`, remove `AFFAIRS` (migration + seed, one demo manager per building)
- Replace the Phase 0 settings with the CLAUDE.md keys and defaults
- ADMIN CRUD (API + screens) for buildings, floors (optional gender preference), room types, rooms, beds, room assets
- `building_managers` link + assignment screen; BR-14 building scope (404, never 403, for other buildings)
- Seed B3, B5, B6, B8, B9, B10, B13; B6 and B9 with the official rents

Done when: admin creates a room type and a room and gets exactly capacity beds; a room with occupants cannot be deleted; the B6 manager cannot see or change anything in B9 (404).

Nothing from Phase 2+ is built: no registrations, rounds, student profiles, room browser, BR-03 gender locking or invoices.

### Decisions from your answers (2026-09-28)

1. **Managers:** read everything in their own buildings and manage room assets there (add, rename, change status, remove). Everything else is ADMIN-only.
2. **Occupancy:** facility defines a `BedOccupancy` interface, consulted by **both** room deletion and single-bed removal. The Phase 1 production implementation, `EmptyBedOccupancy`, reports no bed as occupied, because no Phase 1 table can place a student in a bed. Phase 2 replaces it with one based on registrations and residences (open item in PROGRESS.md, see Close-out). Tests add a test-only fake that marks beds occupied.
3. **Beds:** creating a room generates exactly `capacity` beds (codes `1`..`N`). ADMIN may afterwards remove a free bed or add one back, as long as the count never exceeds the room type's capacity.
4. **Old-model cleanup:** `AuditSubjectType` and the placeholder menus move to Spec v2 names now (placeholders only, no new screens).

---

## 1. Tests written first

Written before the implementation, in this order: BR-14, capacity, delete protection, then the rest. Fixtures are built **through the HTTP API as ADMIN** (`FacilityFixture`), so the tests compile against the API only (plus the `BedOccupancy` interface) and exercise the real controllers. All classes extend `AbstractIntegrationTest` (real MySQL, real HTTP) unless marked otherwise.

Shared fixture for the facility tests: buildings **B6** and **B9**, each with floor 3 (gender preference MALE), room type "8-student room" (capacity 8, 730,000 VND), room 301 (8 beds) and one asset. Managers: `m6` linked to B6, `m9` linked to B9, `m69` linked to both, `m0` linked to none.

`FacilityEndpoints` (test support) is the single catalogue of every building-scoped facility endpoint: name, method, path built from a building's fixture ids, a valid body, and whether a manager may call it. The BR-14 and access tests are parameterized over it, so an endpoint added later without being catalogued is the only way to miss coverage. Catalogue (24 entries):

```
GET/PUT/DELETE /api/buildings/{b}          GET/POST /api/buildings/{b}/floors
PUT/DELETE /api/floors/{f}                 GET/POST /api/buildings/{b}/room-types
PUT/DELETE /api/room-types/{t}             GET /api/buildings/{b}/rooms
POST /api/floors/{f}/rooms                 GET/PUT/DELETE /api/rooms/{r}
POST /api/rooms/{r}/beds                   PUT/DELETE /api/beds/{bed}
POST /api/rooms/{r}/assets    (manager)    PUT/DELETE /api/room-assets/{a}   (manager)
PUT/DELETE /api/buildings/{b}/managers/{userId}
```
The manager may call the 5 GETs and the 3 asset writes; the other 16 are ADMIN-only. Endpoints without a building (`GET /api/buildings`, `POST /api/buildings`, `GET /api/building-managers`) are tested separately.

### BR-14 - `facility/BuildingScopeTest`
- `"BR-14 GET /api/buildings for the B6 manager returns only B6"`
- `"BR-14 a manager linked to B6 and B9 sees both buildings and no other"`
- `"BR-14 a manager linked to no building sees an empty list and gets 404 for every building"`
- `"BR-14 the B6 manager gets 404 on {0} for a B9 resource and nothing in B9 changes"` (parameterized over all 24 endpoints; compares a JDBC snapshot of B9's facility rows before and after)
- `"BR-14 the 404 on {0} for a B9 resource is identical to the 404 for an id that does not exist"` (same status, title and detail; no leak)
- `"BR-14 the B6 manager reaches {0} for the matching B6 resource"` (positive control over the manager-allowed endpoints: 2xx, proving the 404 comes from scope, not a broken endpoint)
- `"BR-14 unassigning the B6 manager turns every B6 endpoint into 404 on the next request"` (scope is read from the database per request, not cached in the session)
- `"BR-14 ADMIN sees every building and is never refused by scope (no 403 or 404) on any facility endpoint in B6 or B9"` (a delete may still answer 409 because the fixture building has floors; that is the delete rule, not scope)

### Access - `facility/FacilityAccessTest`
- `"ACCESS the B6 manager gets 403 on ADMIN-only {0} in B6"` (parameterized over the 16 ADMIN-only endpoints; 403 only inside their own building, because scope is checked first)
- `"ACCESS {0} gets 403 on every facility endpoint, for B6, B9 and non-existent ids alike"` (STUDENT, TECHNICIAN, ACCOUNTANT: `@PreAuthorize` refuses them before any lookup, so they never see a 404)
- `"ACCESS a building manager gets 403 on POST /api/buildings and GET /api/building-managers"`
- `"ACCESS an anonymous request to a facility endpoint gets 401"`

### Capacity - `facility/RoomCapacityTest`
- `"CAPACITY creating a room creates exactly as many beds as its room type's capacity, coded 1 to N"`
- `"CAPACITY a bed cannot be added to a room that already has as many beds as its capacity (409)"`
- `"CAPACITY after a bed is removed, one can be added back up to the capacity and gets the lowest free code"`
- `"CAPACITY bed codes are unique within a room (409)"`
- `"CAPACITY a room type's capacity cannot drop below the bed count of a room that uses it (409); raising it adds no beds"`
- `"CAPACITY a room cannot switch to a room type whose capacity is below its bed count (409)"`
- `"CAPACITY a room can only use a room type of its own building (422)"`

### Delete protection - `facility/FacilityDeleteProtectionTest`
- `"DELETE a room with an occupied bed cannot be deleted (409) and keeps its beds and assets"`
- `"DELETE a room without occupants is deleted together with its beds and assets"`
- `"DELETE an occupied bed cannot be removed (409) and stays; a free bed in the same room can still be removed"`
- `"DELETE a floor with rooms cannot be deleted (409)"`
- `"DELETE a room type used by a room cannot be deleted (409)"`
- `"DELETE a building with floors or room types cannot be deleted (409); an empty one can, and its manager links go with it"`

### CRUD - `facility/FacilityCrudTest`
- `"FACILITY ADMIN creates, renames and lists buildings; a duplicate code is rejected (409)"`
- `"FACILITY floors carry an optional gender preference; floor numbers are unique per building (409)"`
- `"FACILITY room types store capacity, area, air conditioning, water heater, bathrooms and monthly rent in VND"`
- `"FACILITY room codes are unique within a building, across floors (409)"`
- `"FACILITY a new room has gender null and the API cannot set it"`
- `"FACILITY the building room list shows floor, room type, rent, amenities, bed count, occupied beds and gender"`
- `"FACILITY invalid requests return 400 with field errors (blank code, capacity 0, negative rent)"`

### Room assets - `facility/RoomAssetTest`
- `"FACILITY ADMIN and the room's building manager can add an asset, change its status and remove it"`
- `"FACILITY creating an asset and each status change write an audit row (ROOM_ASSET, from, to, user)"`

### Manager links - `facility/BuildingManagerAssignmentTest`
- `"MANAGERS ADMIN assigns a building manager to a building and unassigns them"`
- `"MANAGERS a manager can be linked to more than one building, and assigning twice is idempotent"`
- `"MANAGERS only a user with role BUILDING_MANAGER can be assigned (422); an unknown user is 404"`
- `"MANAGERS GET /api/building-managers lists every building manager with their buildings"`

### Migration - `db/MigrationV3Test` (new)
- `"MIGRATION V3 turns AFFAIRS accounts into BUILDING_MANAGER and replaces the Phase 0 settings with the CLAUDE.md keys"` (runs Flyway to V2 on a scratch schema in the same container as root, inserts an AFFAIRS user and edited Phase 0 settings, migrates to V3, asserts, drops the schema)

### Demo seed - `demo/DemoDataServiceTest` (changed and new)
- `"DEMO seedIfEmpty() on an empty database creates admin, student, technician, accountant and one BUILDING_MANAGER per building"` (replaces "... exactly one enabled account per role")
- `"DEMO seedIfEmpty() is idempotent: a second run creates nothing"` (unchanged)
- `"DEMO every seeded account can log in through POST /api/auth/login"` (unchanged; now includes the managers)
- `"DEMO seeds B3, B5, B6, B8, B9, B10 and B13 with floors, room types and rooms whose bed count equals their room type's capacity"`
- `"DEMO each building's room types match the CLAUDE.md Buildings table: capacities, area, air conditioning, water heater and bathrooms"`
- `"DEMO B6 and B9 room types carry the official rents: 6-student 1,050,000, 8-student 730,000, 10-student 550,000"`
- `"DEMO rents outside B6 and B9 are the team's estimates: air-conditioned buildings as B6/B9 per capacity, B10 450,000 / 380,000 / 320,000, B13 650,000 / 480,000"`
- `"DEMO in every building floors 2 and 3 prefer FEMALE, floors 1 and 4 prefer MALE, and other floors have no preference"`
- `"DEMO each demo manager is linked only to their own building, and manager.b6 gets 404 for a B9 room"`

### Existing tests that change (nothing disabled, deleted or weakened)
The Phase 0 keys `offer_hours` and `repair_due_hours_*` no longer exist, so tests that used them switch to an equivalent key; each keeps its assertions.
- `SettingsServiceTest`: `"SETTINGS V3 seeds the nine CLAUDE.md keys with their defaults"` (replaces the V2 test; also asserts the old keys are gone); typed-getter test covers minutes, hours and days (`default_hold_minutes` 30 min, `repair_deadline_hours_low` 7 days, `overdue_days_block_stay_on` 30 days); invalid-value test uses `"two days"` and `"0"` for minutes, `"-1"` for money, `"3.5"` for the threshold; missing-row test uses `default_hold_minutes`.
- `RoleAccessTest`: **no role assertion changes.** `"ROLE {0} gets 403 problem+json on GET /api/settings"` keeps its code; its `@EnumSource` (every role except ADMIN) now runs for STUDENT, BUILDING_MANAGER, TECHNICIAN and ACCOUNTANT (AFFAIRS no longer exists). The other two tests write to `default_hold_minutes` instead of the removed `offer_hours`, so these follow the new key: the path, the display names, the expected type (`MINUTES`), the list size (9 instead of 5), and the value that must stay the same after the forbidden STUDENT PUT (30 instead of 48). STUDENT still gets 403 on that PUT and the value still does not change.
- `CsrfProtectionTest`: same key swap; the value that must stay the same is 30.
- `DmsApplicationTest`: `"INFRA context starts with Flyway V1 to V4 applied and Hibernate ddl-auto=validate passing"`, 9 settings rows.
- `AuditServiceTest`: sample subject types become `REGISTRATION`, `RESIDENCE`, `ROOM_ASSET`; the local sample enum is renamed.
- `ApiExceptionHandlerTest` (+ `ErrorProbeController`): new `"ERROR ConflictException maps to 409"`, `"ERROR InvalidRequestException maps to 422 without a rule"`, `"ERROR DataIntegrityViolationException maps to 409 without SQL details"`; probe messages reworded to Spec v2 examples.
- `AuthFlowTest`: no change (its `@EnumSource(Role.class)` picks up BUILDING_MANAGER and drops AFFAIRS).

**Phase 0 role access is kept.** STUDENT, TECHNICIAN and ACCOUNTANT still get 403 on every ADMIN endpoint (`/api/settings`, checked by the unchanged parameterized test above), and they also get 403 on every new facility endpoint. BUILDING_MANAGER gets 403 on `/api/settings` like every non-admin role. The scope-before-role order (ambiguity 3) applies **only to BUILDING_MANAGER** on building-scoped facility endpoints. ADMIN is never out of scope, and the other roles are stopped by `@PreAuthorize` before any scope check.

---

## 2. Files to create or change

### Migrations (`backend/src/main/resources/db/migration/`)
V2 is already applied on every database, so it is not edited.

**`V3__spec_v2_roles_and_settings.sql` (new)**
```sql
-- Spec v2 removes the AFFAIRS role. Existing accounts become building managers with no building
-- (not deleted: audit_logs.user_id may reference them).
UPDATE users SET role = 'BUILDING_MANAGER' WHERE role = 'AFFAIRS';

-- Spec v2 settings (CLAUDE.md "Settings and their defaults") replace every Phase 0 key.
DELETE FROM settings;
INSERT INTO settings (setting_key, setting_value, description, updated_at) VALUES
  ('water_fee_monthly',            '40000',  'Water fee per month in VND, part of the semester fee (BR-12)', UTC_TIMESTAMP(6)),
  ('equipment_fee',                '300000', 'Equipment fee in VND, charged once in FIRST_TIME rounds (BR-12)', UTC_TIMESTAMP(6)),
  ('electricity_price_per_kwh',    '3000',   'Electricity price in VND per kWh (BR-07)', UTC_TIMESTAMP(6)),
  ('default_hold_minutes',         '30',     'Hold time in minutes pre-filled for a new registration round (BR-05)', UTC_TIMESTAMP(6)),
  ('overdue_days_block_stay_on',   '30',     'Days after issue after which an unpaid electricity invoice blocks stay-on (BR-09)', UTC_TIMESTAMP(6)),
  ('repair_deadline_hours_urgent', '24',     'Repair deadline for urgent tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
  ('repair_deadline_hours_normal', '72',     'Repair deadline for normal tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
  ('repair_deadline_hours_low',    '168',    'Repair deadline for low priority tickets, in hours (BR-10)', UTC_TIMESTAMP(6)),
  ('warning_threshold',            '3',      'Violations in one term that create a warning review for the Centre Administrator (BR-11)', UTC_TIMESTAMP(6));
```

**`V4__facilities.sql` (new)** - InnoDB, utf8mb4, plain FKs without cascade (services delete children explicitly, so later history rows such as `registrations.bed_id` also block deletes at database level):
```
buildings          id PK, code VARCHAR(16) UNIQUE, name VARCHAR(128)
floors             id PK, building_id FK, number INT, gender_preference VARCHAR(10) NULL, UNIQUE(building_id, number)
room_types         id PK, building_id FK, name VARCHAR(64), capacity INT, area_m2 DECIMAL(6,2), has_air_conditioning BOOLEAN,
                   has_water_heater BOOLEAN, bathrooms INT, monthly_rent BIGINT, UNIQUE(building_id, name)
rooms              id PK, floor_id FK, room_type_id FK, code VARCHAR(16), gender VARCHAR(10) NULL, UNIQUE(floor_id, code)
beds               id PK, room_id FK, code VARCHAR(8), UNIQUE(room_id, code)
room_assets        id PK, room_id FK, name VARCHAR(128), status VARCHAR(20), INDEX(room_id)
building_managers  user_id FK users, building_id FK buildings, PRIMARY KEY(user_id, building_id), INDEX(building_id)
```
Room codes are unique per building (checked in the service; the database can only enforce per floor).

### Backend main (`backend/src/main/java/vn/edu/hust/dms/`)

**common (changed)**
- `common/user/Role.java` - `STUDENT, ADMIN, BUILDING_MANAGER, TECHNICIAN, ACCOUNTANT`
- `common/user/UserAccountRepository.java` - add `findByRoleOrderByFullNameAsc(Role)`
- `common/settings/SettingKey.java` - the nine keys: `WATER_FEE_MONTHLY`, `EQUIPMENT_FEE`, `ELECTRICITY_PRICE_PER_KWH` (MONEY), `DEFAULT_HOLD_MINUTES` (MINUTES), `OVERDUE_DAYS_BLOCK_STAY_ON` (DAYS), `REPAIR_DEADLINE_HOURS_URGENT/NORMAL/LOW` (HOURS), `WARNING_THRESHOLD` (INTEGER)
- `common/settings/SettingType.java` - add `MONEY` (VND, 0 or more), `MINUTES` (1 or more), `DAYS` (0 or more)
- `common/settings/SettingsService.java` - `getDuration` supports MINUTES, HOURS and DAYS
- `common/audit/AuditSubjectType.java` - `REGISTRATION, RESIDENCE, INVOICE, REPAIR_TICKET, WARNING_REVIEW, ROOM_ASSET, USER`
- `common/error/ConflictException.java` (new, 409: duplicate code, resource in use, capacity reached)
- `common/error/InvalidRequestException.java` (new, 422 without a rule id: e.g. room type of another building, non-manager assigned)
- `common/error/ApiExceptionHandler.java` - map `DataIntegrityViolationException` to 409 "The change conflicts with existing data" (safety net for unique-key races; no SQL in the body)

**facility (new)**
- `facility/entity/`: `Building`, `Floor`, `RoomType`, `Room`, `Bed`, `RoomAsset`, `BuildingManager` + `BuildingManagerId` (`@EmbeddedId`, ids only), enums `Gender` (MALE, FEMALE; Phase 2 reuses it for students) and `RoomAssetStatus` (GOOD, DAMAGED, MISSING). Associations `@ManyToOne(fetch = LAZY)`; enums `@Enumerated(STRING)` + `@JdbcTypeCode(SqlTypes.VARCHAR)`; money `long`.
- `facility/repository/`: one repository per entity, with the scope queries (`findBuildingIdById` for floor, room type, room, bed, asset), existence checks for delete protection, grouped bed counts per room, and `BuildingManagerRepository.findBuildingIdsByUserId`.
- `facility/service/BuildingScope.java` - BR-14. `visible()` (all for ADMIN, linked ids for BUILDING_MANAGER, none otherwise), `requireVisible(buildingId, notFoundDetail)` throwing `NotFoundException` with the same generic detail as a missing id ("Room not found"), `requireAdmin()` throwing `AccessDeniedException` (403). Reads `building_managers` on every call. Later phases reuse it.
- `facility/service/BedOccupancy.java` - interface: `Set<Long> occupiedBedIds(Collection<Long> bedIds)`
- `facility/service/EmptyBedOccupancy.java` - the Phase 1 production implementation: returns an empty set for any input. Its Javadoc says why (no Phase 1 table can place a student in a bed) and that Phase 2 replaces it.
- `facility/service/OccupancyService.java` - union of every `BedOccupancy` bean. Called by `RoomService.deleteRoom` and `RoomService.removeBed`, and for the occupied flags and counts in the responses.
- `facility/service/BuildingService`, `FloorService`, `RoomTypeService`, `RoomService` (rooms and beds), `RoomAssetService` (audits create and status changes), `BuildingManagerService`. Each write: load or 404, `requireVisible`, `requireAdmin` where needed, validate (409/422), change. Read methods build the response records inside the read-only transaction (`open-in-view` is off).
- `facility/web/`: `BuildingController`, `FloorController`, `RoomTypeController`, `RoomController` (rooms and beds), `RoomAssetController`, `BuildingManagerController`, each with class-level `@PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")` (`POST /api/buildings` and `GET /api/building-managers` carry `hasRole('ADMIN')`). DTO records: `BuildingRequest/Response`, `FloorRequest/Response`, `RoomTypeRequest/Response`, `CreateRoomRequest`, `UpdateRoomRequest`, `RoomSummaryResponse`, `RoomDetailResponse`, `BedRequest/Response`, `RoomAssetRequest/Response`, `BuildingManagerResponse`.

**demo (changed)**
- `demo/DemoAccount.java` - drop AFFAIRS; add seven managers `manager.b3` ... `manager.b13` (Vietnamese names) with a `buildingCode`
- `demo/DemoFacilities.java` (new) - the seed layout as data, from section 3 item 9: room types from the CLAUDE.md Buildings table; rents official for B6/B9 and `// ESTIMATE` everywhere else; floors, rooms per floor, the room-type split and the assets `// ESTIMATE`; floor gender preferences commented as coming from the resident interview
- `demo/DemoDataService.java` - after the accounts, seeds the facilities by calling the facility services while authenticated as the seeded admin (so the seed obeys the same capacity, uniqueness and scope rules as the API), then links each manager to their building

### Backend tests (`backend/src/test/java/vn/edu/hust/dms/`)
- `support/DatabaseCleaner.java` - keeps one JVM-wide (static) snapshot of the `settings` rows, taken on the first clean, when the container's database is fresh from Flyway, and re-inserts it after each truncate, instead of re-running `V2__seed_settings.sql` (which would now bring back the old keys). Static, so a second Spring context cannot snapshot settings a test has changed.
- `support/AbstractIntegrationTest.java` - also clears `TestBedOccupancy` before each test
- `support/TestBedOccupancy.java` (new) - test-only `BedOccupancy` bean with `occupy(bedId)` / `clear()`, active next to `EmptyBedOccupancy` (union)
- `support/FacilityFixture.java` (new) - builds B6/B9 through the API as ADMIN, links managers, JDBC snapshot of a building's rows
- `support/FacilityEndpoints.java` (new) - the endpoint catalogue
- `support/ErrorProbeController.java` - probes for the new exceptions
- new test classes: `facility/BuildingScopeTest`, `FacilityAccessTest`, `RoomCapacityTest`, `FacilityDeleteProtectionTest`, `FacilityCrudTest`, `RoomAssetTest`, `BuildingManagerAssignmentTest`, `db/MigrationV3Test`
- changed: `SettingsServiceTest`, `RoleAccessTest`, `CsrfProtectionTest`, `DmsApplicationTest`, `AuditServiceTest`, `ApiExceptionHandlerTest`, `DemoDataServiceTest`

### Frontend (`frontend/src/`)
- `api/types.ts` - roles (add BUILDING_MANAGER "Building manager", remove AFFAIRS)
- `api/facilityTypes.ts` (new), `api/facilities.ts` (new) - typed calls and query keys for every endpoint above
- `lib/format.ts` (new) - `formatVnd`, gender and status labels
- `components/` (new): `Modal`, `ConfirmDialog`, `ErrorBanner` (ApiError detail and field errors), `Badge`, `PageHeader`
- `pages/facilities/BuildingListPage.tsx` - ADMIN: all buildings with create/edit/delete; manager: "My buildings"
- `pages/facilities/BuildingDetailPage.tsx` (header shows the building's managers, read-only) with panels `RoomsPanel` (grouped by floor: type, rent, amenities, beds, occupied, gender), `FloorsPanel`, `RoomTypesPanel`
- `pages/facilities/RoomDetailPage.tsx` - room info, beds (ADMIN: add up to capacity, relabel, remove), assets (ADMIN and manager); an API 404 renders `NotFoundPage`
- `pages/facilities/forms/`: `BuildingForm`, `FloorForm`, `RoomTypeForm`, `RoomForm`, `BedForm`, `AssetForm`
- `pages/managers/BuildingManagersPage.tsx` - the assignment screen (ADMIN): every manager with a checkbox per building
- `layout/navigation.ts` - Spec v2 menus (placeholders keep "Coming in Phase N"):
  - STUDENT: Home, My declaration (2), Registration (2), My residence (3), Invoices (4), Repair tickets (5), Announcements (5)
  - ADMIN: Home, Facilities (1), Building managers (1), Registration rounds (2), Warning reviews (5), Reports (6), Settings (6), Audit log (6)
  - BUILDING_MANAGER: Home, My buildings (1), Check-in (3), Meter readings (4), Repair tickets (5), Announcements (5), Violations (5)
  - TECHNICIAN: Home, My tickets (5); ACCOUNTANT: Home, Invoices (4), Payments (4)
- `app/router.tsx` - real routes `/facilities`, `/facilities/buildings/:buildingId`, `/facilities/rooms/:roomId` (`RequireRole` ADMIN and BUILDING_MANAGER) and `/building-managers` (ADMIN); placeholders only for items of phase 2+. The UI hides ADMIN-only buttons from managers; the backend stays the enforcement.

### Docs
- `README.md` - project description in Spec v2 terms (it still says "admission, bed assignment, contract"), demo accounts (no `affairs`; seven managers), reset note for Phase 0 databases
- `docs/PROGRESS.md` - Phase 1 entry at close-out
- `docs/plans/phase-1.md` - copy of this plan at close-out
- `CLAUDE.md` (team-approved):
  1. A new `### Buildings` section inserted directly below the settings table, before the existing paragraph on seed rents. That paragraph stays unchanged. Rents stay out of the table, because only B6/B9 are known; the estimates live in the seeder.
     ```
     ### Buildings

     Room types per building, from resident interviews.

     | Building | Capacities | Area m2 | Air con | Water heater | Bathrooms |
     |---|---|---|---|---|---|
     | B3  | 10        | 38 | yes | yes | 1 |
     | B5  | 8, 10     | 38 | yes | yes | 1 |
     | B6  | 6, 8, 10  | 38 | yes | yes | 1 |
     | B8  | 6, 8      | 30 | yes | yes | 1 |
     | B9  | 6, 8, 10  | 38 | yes | yes | 1 |
     | B10 | 8, 10, 12 | 60 | no (2 ceiling fans) | yes | 2 |
     | B13 | 6, 8      | 30 | no (fan) | yes | 1 |
     ```
  2. The column details accepted in ambiguities 1 and 2, added to the domain model: `buildings (code, name)`, `floors (building_id, number, gender_preference)`, `beds (room_id, code)`, `room_assets.status: GOOD | DAMAGED | MISSING`, room code unique per building.

---

## 3. Ambiguities - decided by the team (2026-09-28)

The team accepted the defaults of items 1 to 8 and 10 to 13 as written. Item 9 was replaced with resident data, and the seed's floor gender preferences come from the resident interview.

1. **Room asset statuses.** CLAUDE.md names `room_assets.status` without values. Default: `GOOD | DAMAGED | MISSING`; creation and every status change write an audit row (`ROOM_ASSET`). No quantity column (one row per item).
2. **Columns the domain model leaves open.** Default: `buildings(code, name)`, `floors.number`, `beds.code` (default = lowest free number), room code unique per building (e.g. `301`), `area_m2 DECIMAL(6,2)`.
3. **404 before 403 (BUILDING_MANAGER only).** Default: for a building manager, scope is checked before the ADMIN-only check, so the B6 manager gets 404 on **every** B9 endpoint, including ADMIN-only ones; 403 only appears for an ADMIN-only action inside their own building. This matches BR-14 literally ("any other building returns 404"). STUDENT, TECHNICIAN and ACCOUNTANT keep the Phase 0 behaviour: 403 from `@PreAuthorize`, for any id. Alternative: 403 whenever the role lacks the permission, anywhere.
4. **Delete rules.** Default: delete bottom-up. A building needs no floors or room types (its manager links go with it), a floor needs no rooms, a room type must be unused, a room must have no occupants (its beds and assets go with it). Once Phase 2 has registration history, rooms whose beds have history can no longer be hard-deleted; Phase 2 must choose "block" or "archive".
5. **Capacity changes.** Default: lowering a room type's capacity below a room's bed count is 409; raising it adds no beds (ADMIN adds them); a room may switch type only if its bed count fits. Rent edits apply only to invoices issued later (BR-08).
6. **Old AFFAIRS accounts.** Default: V3 converts them to BUILDING_MANAGER with no building (kept for the audit FK). Alternative: also disable them.
7. **Creating manager accounts.** BUILD_PLAN has no user management. Default: Phase 1 only assigns existing BUILDING_MANAGER users (seeded); creating accounts waits for a phase the team names.
8. **Several managers per building.** The link table allows it and CLAUDE.md does not forbid it. Default: allowed; the seed has one each.
9. **Seed data (decided by the team, replaces my estimates).** Capacities, area, amenities and bathrooms come from residents and also go into CLAUDE.md as the Buildings table (see Docs). Rents are official only for B6/B9; every other rent is marked `ESTIMATE`. Each room type's `area_m2` is its building's area.

   | Building | Capacities | Area m2 | Air con | Water heater | Bathrooms | Monthly rent VND by capacity |
   |---|---|---|---|---|---|---|
   | B3 | 10 | 38 | yes | yes | 1 | 10: 550,000 (ESTIMATE, as B6/B9) |
   | B5 | 8, 10 | 38 | yes | yes | 1 | 8: 730,000; 10: 550,000 (ESTIMATE, as B6/B9) |
   | B6 | 6, 8, 10 | 38 | yes | yes | 1 | 6: 1,050,000; 8: 730,000; 10: 550,000 (official) |
   | B8 | 6, 8 | 30 | yes | yes | 1 | 6: 1,050,000; 8: 730,000 (ESTIMATE, as B6/B9) |
   | B9 | 6, 8, 10 | 38 | yes | yes | 1 | 6: 1,050,000; 8: 730,000; 10: 550,000 (official) |
   | B10 | 8, 10, 12 | 60 | no (2 ceiling fans) | yes | 2 | 8: 450,000; 10: 380,000; 12: 320,000 (ESTIMATE) |
   | B13 | 6, 8 | 30 | no (fan) | yes | 1 | 6: 650,000; 8: 480,000 (ESTIMATE) |

   - **Floor gender preferences** (resident interview), in every building: FEMALE on floors 2 and 3, MALE on floors 1 and 4, none on other floors.
   - **Still estimates**, marked `ESTIMATE` as proposed:
     - 5 floors of 6 rooms each (B3 and B5: 4 floors); room codes `101`..`506`
     - a floor's rooms split evenly across the building's capacities (B6: two each of 6, 8 and 10)
     - assets per room, following the amenities: an air conditioner in the air-conditioned buildings, two ceiling fans in B10, one fan in B13, plus a water heater and a wardrobe everywhere; all GOOD except a few DAMAGED
   - About 200 rooms and 1,650 beds in total.
10. **Phase 0 dev databases.** CLAUDE.md seeds only an empty database, so a teammate's existing Phase 0 database gets V3/V4 but no buildings. Default: keep the rule; reset with `docker compose down -v`. Alternative: seed facilities whenever `buildings` is empty.
11. **Setting value ranges.** Default: money 0 or more, `default_hold_minutes` 1 or more, days and hours 0 or more, `warning_threshold` an integer. `default_hold_minutes` only pre-fills a new round; each round keeps its own `hold_minutes`.
12. **Who else reads facility data.** Default: nobody in Phase 1 (the student room browser comes in Phase 2, technicians in Phase 5); STUDENT, TECHNICIAN and ACCOUNTANT get 403.
13. **Residences in the Phase 2 occupancy item.** The PROGRESS item (Close-out) asks Phase 2 for an implementation based on registrations *and residences*, but BUILD_PLAN only creates residences in Phase 3 (a payment creates the first one). Default: write the item exactly as given. If Phase 2 does not create the `residences` table, Phase 2 covers registrations and Phase 3 adds residences to the same implementation.

Phase 0 open questions this closes: meter readings belong to the building manager (BUILD_PLAN Phase 4), so the placeholder moves there; BR-09's day count is the setting `overdue_days_block_stay_on`.

---

## Design notes

### Permission matrix
| Endpoint group | ADMIN | Manager, own building | Manager, other building | Other roles |
|---|---|---|---|---|
| all GETs, incl. `GET /api/buildings` (filtered) | all | 200 | 404 | 403 |
| asset POST/PUT/DELETE | yes | yes | 404 | 403 |
| every other building-scoped write | yes | 403 | 404 | 403 |
| `POST /api/buildings`, `GET /api/building-managers` | yes | 403 | 403 | 403 |

STUDENT, TECHNICIAN and ACCOUNTANT never reach a facility service: the class-level `@PreAuthorize` answers 403 first, for any id, exactly like the Phase 0 ADMIN endpoints. ADMIN passes every scope check. So the ordering below matters only for BUILDING_MANAGER: 404 if missing, 404 if out of scope, 403 if the action is ADMIN-only, then 409/422. Bean validation of the body runs before the service, so a malformed body is 400 everywhere; that leaks nothing about existence.

### Beds and occupancy
- `RoomService.create` generates `capacity` beds in the same transaction. `addBed` loads the room with `PESSIMISTIC_WRITE`, then checks `count < capacity`.
- Two guards ask `OccupancyService`, each after taking `PESSIMISTIC_WRITE` locks:
  - `removeBed` locks that bed; if it is occupied, it answers 409 and the bed stays.
  - `deleteRoom` locks all the room's beds; if any one is occupied, the whole room is refused with 409.

  Because of the locks, a Phase 2 hold on one of those beds (BR-04 locks the bed too) waits for the delete. The plain FKs from later history tables are the backstop.
- Occupied = any `BedOccupancy` bean reports the bed (union).
- **In production in Phase 1** the only bean is `EmptyBedOccupancy`, which returns an empty set for any input. Every bed is free, the API's occupied flags and counts are always false/0, and neither guard ever refuses in the running app; they can only be seen refusing in the tests, through `TestBedOccupancy`.
- The union, rather than injecting one bean, lets the test fake run next to Phase 2's real implementation without hiding it.
- Phase 2 replaces `EmptyBedOccupancy` (open item below); the real implementation counts HELD, CONFIRMED and CHECKED_IN registrations and ACTIVE residences (BR-04).
- `rooms.gender` exists and is always read-only here; BR-03 sets it in Phase 2.

### Order of work
1. CLAUDE.md (Buildings table, domain model details), V3, Role, settings, audit types, and the updated Phase 0 tests (suite green again)
2. `BedOccupancy` interface, test support, then the failing tests of section 1
3. V4, entities, repositories, `BuildingScope`, `EmptyBedOccupancy` + `OccupancyService`, services, controllers until green
4. Demo seed
5. Frontend
6. README, PROGRESS, plan copy

---

## Verification

1. Environment (memory note): `export JAVA_HOME="/c/Users/Admin/.jdks/jdk-21.0.12.1+1"; export PATH="$JAVA_HOME/bin:$PATH"`, start Docker Desktop, wait for `docker info`.
2. `cd backend && ./mvnw test`: every test in section 1 and the full Phase 0 suite green on Testcontainers MySQL; nothing skipped.
3. `cd frontend && npm run build && npm run lint` pass.
4. Reset the dev database (`docker compose down -v && docker compose up -d`; this wipes the local dev data), run `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo`: log shows Flyway V1 to V4 and the seed of 7 buildings.
5. Manual "Done when" in the UI (http://localhost:5173):
   - as `admin`: in B6 create room type "Renovated 4-student room" (capacity 4; the seeded names are taken), create room `507` on floor 5 with it, open it: 4 beds; adding a 5th is refused; remove one and add it back
   - as `admin`: open the building managers screen, assign and unassign a manager
   - as `manager.b6`: only B6 in "My buildings"; change an asset status; open a B9 room URL (`/facilities/rooms/<id>`): 404 page
6. API check of 404 for B9 as `manager.b6` (curl with the cookie jar, as in the Phase 0 plan): `GET /api/rooms/<B9 room>` and `PUT /api/room-assets/<B9 asset>` return 404 problem+json with the same detail as a non-existent id.
7. "Room with occupants cannot be deleted" and its single-bed counterpart: proven by `FacilityDeleteProtectionTest` through `TestBedOccupancy`. With `EmptyBedOccupancy` no bed is ever occupied in the running app until Phase 2, so in the UI only the positive side can be shown: deleting an empty room and removing a free bed both work.

## Close-out (WORKFLOW step 6, when you send that prompt)
Append the Phase 1 entry to `docs/PROGRESS.md` (built, BR-14 tests passing, decisions, open questions), copy this plan to `docs/plans/phase-1.md`, commit "Phase 1: facilities, room types and building managers". The team then takes the room list and room detail screenshots for Claude Design.

The entry's **Open questions** will contain this item, word for word:

> Phase 2 must replace the Phase 1 BedOccupancy implementation with one based on registrations and residences, and add an integration test that deleting a room or removing a bed with a HELD registration fails.

(The Phase 2/3 note on residences stays in ambiguity 13 of this plan and is not added to the item unless the team asks.)
