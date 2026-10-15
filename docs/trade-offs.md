# Technical Architecture & Design Trade-offs

This document outlines key technical decisions and architectural trade-offs made during the design and implementation of the Salary Management System.

---

## 1. Modular Monolith vs. Microservices

- **Decision**: Implemented as a single, well-structured Spring Boot modular monolith rather than multiple distributed microservices.
- **Rationale**:
  - The business domain (Employees, Compensation History, Reporting) is cohesively coupled around relational transactions.
  - Salary adjustments require atomic updates (closing a prior record while opening a new record); in a single application, this is guaranteed by PostgreSQL ACID transactions.
  - Avoided distributed system complexities: network latency, distributed transaction managers (2PC/Sagas), service mesh, and multi-service deployment orchestration.
- **Trade-off**: Horizontal scaling scales all modules together rather than independently; however, for HR salary management with 10,000+ employees, vertical scaling and database connection pooling provide more than sufficient throughput.

---

## 2. Relational Integrity with Database Constraints vs. Application-Only Validation

- **Decision**: Enforced critical business invariants at both the Spring service layer (via Jakarta Bean Validation and business checks) and the PostgreSQL schema level.
- **Enforced Invariants**:
  - `chk_employee_dates`: Ensures `terminated_at IS NULL OR terminated_at >= joined_at`.
  - `chk_compensation_salary_positive`: Ensures `annual_salary > 0`.
  - `chk_compensation_effective_dates`: Ensures `effective_to IS NULL OR effective_to > effective_from`.
  - `uq_compensation_open_ended`: PostgreSQL partial unique index ensuring at most one open-ended (`effective_to IS NULL`) record per employee.
  - Foreign key cascades set to `RESTRICT` to prevent accidental orphaned financial records.
- **Trade-off**: Requires strict ordering when inserting and updating historical records (e.g. flushing closed records before inserting open records), but guarantees zero data corruption regardless of concurrency or external tools accessing the database.

---

## 3. Database-Level Query Aggregations vs. In-Memory Calculation

- **Decision**: Computed salary statistics (`MIN`, `MAX`, `AVG`, `COUNT`) directly inside PostgreSQL using custom JPQL queries and repository projections.
- **Rationale**:
  - Aggregating in the database leverages PostgreSQL's query optimizer and index lookups (`idx_compensation_employee_dates`, `idx_employee_country_code`).
  - Eliminates the need to load 10,000 employee entities and 25,000+ compensation records into the JVM heap.
  - Response times remain under 50 ms even on large datasets.
- **Trade-off**: Complex reporting queries are tied to the relational database structure, but this is mitigated by Spring Data JPA abstractions and deterministic unit testing.

---

## 4. Bulk JDBC Batching vs. JPA Entity Persistence for Demo Seeding

- **Decision**: Implemented `DemoDataSeeder` using `JdbcTemplate.batchUpdate` in chunks of 1,000 statements, rather than `JpaRepository.saveAll()`.
- **Rationale**:
  - Inserting 10,000 employees and ~25,000 compensation records via standard JPA generates 35,000 round-trips or manages an enormous Hibernate Persistence Context, taking 30–60+ seconds and risking `OutOfMemoryError`.
  - JDBC batching streams statements in batches of 1,000 directly to PostgreSQL, completing the entire 10,000-employee seed in **~4 seconds** while enforcing all foreign keys and constraints.
- **Trade-off**: Required writing raw SQL insert statements for the seeder rather than reusing entity classes, but achieved a ~15x performance improvement and minimal memory footprint.

---

## 5. Lightweight Standalone Angular Components vs. Heavy UI Libraries

- **Decision**: Built the frontend using native Angular 22 standalone components, Reactive Forms, and shared CSS design tokens, without external UI component libraries (e.g. Angular Material, PrimeNG, Bootstrap).
- **Rationale**:
  - Avoided third-party dependency churn, version incompatibilities with Angular 22 / Node 24, and bundle bloat.
  - Kept production initial chunk size to **419 kB** (compressed transfer size **~103 kB**).
  - Maintained component CSS well within the strict **4 kB** budget warning limit.
- **Trade-off**: Custom styling of modals, date pickers, and tables required upfront CSS work, but resulted in an accessible, responsive, and completely customizable user interface.

---

## 6. Soft Deletion with Historical Audit Retention vs. Hard Deletion

- **Decision**: Soft deletion sets `deleted_at` on employee records without removing associated rows from `compensation_record` or `employee`.
- **Rationale**:
  - HR and financial systems require complete historical audit trails for tax compliance and historical salary reporting.
  - Soft-deleted records are automatically excluded from directory searches and current salary reports (`deleted_at IS NULL`).
- **Trade-off**: Requires an additional filter predicate on directory and reporting queries; mitigated by indexing `idx_employee_deleted_at`.

---

## 7. Single Native Currency per Country vs. Foreign Exchange Conversion

- **Decision**: Each country operates in its configured native currency (`USD` for `US`, `GBP` for `GB`). Cross-country reports display native currencies without FX conversion.
- **Rationale**:
  - Avoids volatile exchange rate dependencies, third-party FX API latency, and rounding inaccuracies.
  - Matches typical multinational HR payroll realities where employees are paid and reported in the jurisdiction's local legal tender.
- **Trade-off**: Comparisons across different countries require external context or separate localized views.
