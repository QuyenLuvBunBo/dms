# DMS - Dormitory Management System

University group project (System Analysis and Design, HUST, Group 7). A web system for the HUST dormitory that models the current registration process (priority windows, self-service room choice, 30-minute payment holds, semester prepayment) and extends it into the residence lifecycle: check-in and residence declaration, monthly electricity sharing, repairs, announcements and warnings.

- `CLAUDE.md` - architecture, domain model, business rules and conventions
- `docs/BUILD_PLAN.md` - the phases
- `docs/PROGRESS.md` - what is done
- `docs/WORKFLOW.md` - how each phase is run

## Prerequisites

- JDK 21 (`java -version`)
- Node.js 20+ and npm
- Docker Desktop, running (the backend tests start a real MySQL through Testcontainers)
- Git

## Run

```bash
docker compose up -d                                                       # MySQL 8.4 on localhost:3306 (db, user and password: dms)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo   # API on http://localhost:8080
cd backend && ./mvnw test                                                  # backend test suite (needs Docker)
cd frontend && npm install && npm run dev                                  # UI on http://localhost:5173, proxies /api to :8080
```

PowerShell: use `.\mvnw.cmd` instead of `./mvnw` and quote the profile flag: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,demo"`.

The `demo` profile seeds demo data on startup when the database is empty: the accounts below and buildings B3, B5, B6, B8, B9, B10 and B13 with their floors, room types, rooms, beds and room assets.

A database created before Phase 1 already has accounts, so the seed skips it and it has no buildings. Reset it with `docker compose down -v && docker compose up -d` (this deletes the local data), then start the backend again.

## Demo accounts (`demo` profile, password `password`)

| Username      | Role             | Name           | Building |
|---------------|------------------|----------------|----------|
| `admin`       | ADMIN            | Trần Thị Mai   | all      |
| `student`     | STUDENT          | Nguyễn Văn An  |          |
| `technician`  | TECHNICIAN       | Lê Văn Bình    |          |
| `accountant`  | ACCOUNTANT       | Phạm Thị Hoa   |          |
| `manager.b3`  | BUILDING_MANAGER | Đỗ Văn Hùng    | B3       |
| `manager.b5`  | BUILDING_MANAGER | Vũ Thị Lan     | B5       |
| `manager.b6`  | BUILDING_MANAGER | Hoàng Minh Đức | B6       |
| `manager.b8`  | BUILDING_MANAGER | Bùi Thị Thu    | B8       |
| `manager.b9`  | BUILDING_MANAGER | Đặng Văn Nam   | B9       |
| `manager.b10` | BUILDING_MANAGER | Phan Thị Hằng  | B10      |
| `manager.b13` | BUILDING_MANAGER | Trịnh Văn Long | B13      |

A building manager sees and changes only their own building (BR-14); any other building answers 404.
