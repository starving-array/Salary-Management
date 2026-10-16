# Salary Management System

A production-grade HR and Salary Management platform with a Spring Boot 3.4 REST backend, PostgreSQL 16 database, and Angular 22 standalone frontend.

---

## Architecture & Technology Stack

- **Backend**: Java 21, Spring Boot 3.4, Spring Data JPA, Flyway migrations, Jakarta Validation, PostgreSQL driver.
- **Database**: PostgreSQL 16 (dockerized or local).
- **Frontend**: Angular 22 standalone components, Reactive Forms, TypeScript, Vitest.

---

## Getting Started

### Prerequisites

- **Java**: JDK 21+
- **Node.js**: v20+ and npm
- **Docker**: Docker Desktop / Engine with Docker Compose

### 1. Database Setup

Start PostgreSQL via Docker Compose:

```bash
docker compose up -d
```

PostgreSQL will run on port `5432` with database `salary_management`, user `postgres`, and password `postgres`.

#### Database Configuration & Environment Variables

The backend connects to PostgreSQL using the following environment variables (with default fallbacks):

| Environment Variable | Description | Default Value |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC Connection URL | `jdbc:postgresql://localhost:5432/salary_management` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |

### 2. Backend Setup

Flyway automatically validates and applies version-controlled migrations (`V1__initial_schema.sql`, `V2__add_compensation_status.sql`) on startup.

Start the backend:

```bash
./backend/gradlew -p backend bootRun
```
*(On Windows: `.\backend\gradlew.bat -p backend bootRun`)*

The API will be accessible at `http://localhost:8080/api/v1`.

> [!NOTE]
> Normal backend startup (`bootRun`) starts only the HTTP web server and **does not automatically seed demo data**.

### 3. Frontend Setup

Install dependencies and start the Angular development server:

```bash
cd frontend
npm install
npm start
```

The frontend portal will be available at `http://localhost:4200`.

---

## Deterministic Demo Data Seeding (Phase 6)

The application provides a deterministic, high-performance demo data seeder that populates PostgreSQL with **10,000 fictional employees** and **realistic salary histories** (~25,000 compensation records) in **under 6 seconds** using JDBC batching.

### Key Characteristics

1. **Architecturally Standalone**: Seeding executes as a dedicated non-web process (`DemoDataSeedApplication`) configured with `WebApplicationType.NONE`. It connects to PostgreSQL, validates migrations, runs the deterministic seed, and exits without starting an embedded Tomcat server or binding port 8080.
2. **Safe & Independent**: Normal backend startup (`bootRun`) starts only the API server and never seeds demo data. Seeding and backend execution are fully independent—seeding can run while another backend instance is actively serving requests on port 8080.
3. **Deterministic & Realistic**: Generates reproducible employee numbers (`EMP-00001` through `EMP-10000`), unique emails, varied names across 10 departments and 29 job titles, country distribution (`US` ~60%, `GB` ~40%), employment statuses (Active, Terminated, Soft-deleted), and valid non-overlapping compensation history adhering to all business rules.
4. **Idempotent**: If the 10,000 demo employees are already present, executing the seed command safely detects existing data and skips re-insertion without side effects.
5. **Safety-Guarded Reset**: Resetting demo data requires explicit confirmation to prevent accidental data loss.

### Seeding Commands

#### Seed Demo Data (Standalone Process)

```bash
./backend/gradlew -p backend seedDemoData
```
*(On Windows: `.\backend\gradlew.bat -p backend seedDemoData`)*

- Connects to PostgreSQL, validates Flyway schema migrations, and deterministically inserts 10,000 employees and ~25,000 compensation records.
- Operates as a non-web process without port binding; works safely even if the backend is already running on port 8080.
- Skips insertion automatically if the dataset is already present to preserve idempotency.

#### Reset and Re-seed Demo Data (Destructive with Safety Guard)

```bash
./backend/gradlew -p backend resetAndSeedDemoData
```
*(On Windows: `.\backend\gradlew.bat -p backend resetAndSeedDemoData`)*

- **Destructive effect**: Deletes existing demo records matching `employee_number LIKE 'EMP-%'` and their compensation records. Non-demo user records are preserved.
- **Safety guard**: Requires explicit authorization (`--seed.demo-data.reset=true` and `--seed.demo-data.confirm=true`). If reset is attempted without the explicit confirmation guard, the process aborts immediately with an `IllegalStateException` to prevent accidental deletion.
- Re-populates with a fresh deterministic 10,000-employee dataset.

---

## Running Tests

- **Backend Test Suite** (Unit & Integration Tests):
  ```bash
  ./backend/gradlew -p backend test
  ```

- **Frontend Test Suite** (Vitest):
  ```bash
  cd frontend
  npm test -- --no-watch
  ```

- **Frontend Production Build**:
  ```bash
  cd frontend
  npm run build
  ```

---

## Documentation Index

- [Requirements Specification](docs/requirements.md) — Functional and non-functional requirements and acceptance criteria.
- [Architecture Overview](docs/architecture.md) — System boundaries, tech stack, and module design.
- [Domain Design](docs/design.md) — Entity relationships, effective-date rules, and reporting logic.
- [Technical Trade-offs](docs/trade-offs.md) — Architectural decisions, alternatives considered, and trade-off rationales.
- [Performance & Benchmarks](docs/performance.md) — Empirical seed metrics, API latencies, indexing strategy, and bundle sizes.
- [AI Usage Notes](docs/ai-usage.md) — Methodological notes on AI assistance and engineering oversight.
