# Salary Management System

A production-grade HR and Salary Management platform with a Spring Boot 3.4 REST backend, PostgreSQL 16 database, and Angular 22 standalone frontend.

---

## Architecture & Technology Stack

- **Backend**: Java 21, Spring Boot 3.4, Spring Data JPA, Flyway migrations, Jakarta Validation, PostgreSQL driver.
- **Database**: PostgreSQL 16 (dockerized or local).
- **Frontend**: Angular 22 standalone components, Reactive Forms, TypeScript, Vitest.

---

## Getting Started

### 1. Database Setup

Start PostgreSQL via Docker Compose:

```bash
docker compose up -d
```

PostgreSQL will run on port `5432` with database `salary_management`, user `postgres`, and password `postgres`.

### 2. Backend Setup

Run database migrations and start the backend:

```bash
./backend/gradlew -p backend bootRun
```

The API will be accessible at `http://localhost:8080/api/v1`.

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

The application provides a deterministic, high-performance demo data seeder that populates PostgreSQL with **10,000 fictional employees** and **realistic salary histories** (~25,000 compensation records) in **under 5 seconds** using JDBC batching.

### Key Characteristics

1. **Safe by Default**: The seeder is strictly disabled on normal application startup. It never deletes or resets data automatically.
2. **Deterministic & Realistic**: Generates reproducible employee numbers (`EMP-00001` through `EMP-10000`), unique emails, varied names across 10 departments and 29 job titles, country distribution (`US` ~60%, `GB` ~40%), employment statuses (Active, Terminated, Soft-deleted), and valid non-overlapping compensation history adhering to all business rules.
3. **Idempotent**: If the 10,000 demo employees are already present, executing the seed command safely detects existing data and skips re-insertion without side effects.
4. **Safety-Guarded Reset**: Resetting demo data requires explicit confirmation to prevent accidental data loss.

### Seeding Commands

#### Option A: Dedicated Gradle Tasks (Recommended)

- **Seed 10,000 Demo Employees**:
  ```bash
  ./backend/gradlew -p backend seedDemoData
  ```
  *Executes deterministic seeding, logs insertion statistics and duration, and exits cleanly with code 0.*

- **Reset and Re-seed Demo Employees**:
  ```bash
  ./backend/gradlew -p backend resetAndSeedDemoData
  ```
  *Explicitly deletes previous `EMP-%` records with safety guards and inserts fresh deterministic data.*

#### Option B: Spring Boot Profile

Start the backend with the `seed` profile active:

```bash
./backend/gradlew -p backend bootRun --args='--spring.profiles.active=seed'
```

#### Option C: Command-Line Flags

```bash
./backend/gradlew -p backend bootRun --args='--seed.demo-data.enabled=true --seed.demo-data.exit-after-seed=true'
```

To perform a safe reset via CLI flags:

```bash
./backend/gradlew -p backend bootRun --args='--seed.demo-data.enabled=true --seed.demo-data.reset=true --seed.demo-data.confirm=true --seed.demo-data.exit-after-seed=true'
```

> [!NOTE]
> If `--seed.demo-data.reset=true` is supplied without `--seed.demo-data.confirm=true`, the process aborts immediately with an `IllegalStateException` to safeguard database integrity.

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
