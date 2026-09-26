# DMS - Dormitory Management System

University group project (System Analysis and Design, HUST, Group 7). A web system covering the full dormitory residence lifecycle: admission, bed assignment, contract, monthly billing, maintenance and discipline, renewal or check-out.

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

The `demo` profile seeds demo data on startup when the database is empty.

## Demo accounts (`demo` profile, password `password`)

| Username     | Role       | Name           |
|--------------|------------|----------------|
| `admin`      | ADMIN      | Trần Thị Mai   |
| `student`    | STUDENT    | Nguyễn Văn An  |
| `technician` | TECHNICIAN | Lê Văn Bình    |
| `accountant` | ACCOUNTANT | Phạm Thị Hoa   |
| `affairs`    | AFFAIRS    | Hoàng Minh Đức |
