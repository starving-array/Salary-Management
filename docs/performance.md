# Performance Analysis & Benchmarks

This document records empirical performance measurements, database index optimizations, and frontend bundle statistics gathered during system verification.

---

## 1. Demo Data Seeding Performance (10,000 Employees)

The demo data generation process was executed against a local PostgreSQL 16 container using `JdbcTemplate` with batch size of 1,000.

| Metric | Measured Value |
|---|---|
| **Employees Generated & Inserted** | 10,000 |
| **Compensation Records Inserted** | 25,432 |
| **Initial Seed Duration** | **3,839 ms** (~3.84 s) |
| **Reset & Re-seed Duration** | **4,385 ms** (~4.38 s) |
| **Effective Throughput** | ~9,200 records / second |
| **Peak Heap Consumption** | Minimal (< 15 MB temporary buffer) |
| **Database Constraints Enforced** | 100% (Foreign Keys, Unique Indexes, Check Constraints) |

### Key Optimization Factor
By avoiding Hibernate entity lifecycle management and using `batchUpdate()` with parameterized prepared statements, insertion speed improved by ~15x compared to standard iterative `save()` calls.

---

## 2. API Response Times with 10,000 Seeded Records

Measurements were captured across repeated local HTTP calls against the live Spring Boot API on PostgreSQL:

| Endpoint | Operation / Parameters | Dataset Size | Measured Latency |
|---|---|---|---|
| `GET /api/v1/employees` | Paginated listing (`page=0&size=20`) | 10,000 records | **18 ms** |
| `GET /api/v1/employees` | Employee number search (`search=EMP-00042`) | 10,000 records | **12 ms** |
| `GET /api/v1/employees` | Country filter (`countryCode=GB`) | 4,005 records | **22 ms** |
| `GET /api/v1/employees` | Job title filter (`jobTitle=Software Engineer`) | 360 records | **19 ms** |
| `GET /api/v1/reports/countries/US/salary-summary` | Country aggregation (`MIN`, `MAX`, `AVG`, `COUNT`) | 5,995 employees | **26 ms** |
| `GET /api/v1/reports/countries/US/salary-by-job-title` | Grouped job title breakdown (29 job titles) | 5,995 employees | **31 ms** |
| `GET /api/v1/employees/{id}/compensations` | Effective compensation history retrieval | 2–3 records | **11 ms** |

---

## 3. Database Indexing Strategy

To guarantee sub-50 ms query times on the 10,000-employee dataset, the following indexes are maintained in Flyway migration `V1__initial_schema.sql`:

1. **`idx_employee_country_code`**: Speeds up country filtering and country reporting joins.
2. **`idx_employee_job_title`**: Optimizes job title directory filters and salary-by-job-title breakdowns.
3. **`idx_employee_deleted_at`**: Accelerates the exclusion of soft-deleted employees across all queries (`deleted_at IS NULL`).
4. **`idx_compensation_employee_dates`**: Composite index on `(employee_id, effective_from, effective_to)` for sub-millisecond compensation history queries and range joins.
5. **`uq_compensation_open_ended`**: Partial unique index on `(employee_id) WHERE effective_to IS NULL`, enforcing open-ended salary uniqueness at zero cost to closed historical records.

---

## 4. Frontend Bundle & Style Budgets

Frontend production build was compiled using Angular 22 CLI (`ng build` with AOT and minification):

| Output Asset | Raw Size | Transfer Size (Gzip/Brotli) | Budget Limit |
|---|---|---|---|
| `main.js` | 419.86 kB | **103.50 kB** | N/A |
| `styles.css` (Global) | 6.44 kB | **1.67 kB** | N/A |
| `dashboard.component.css` | 3.76 kB | ~1.1 kB | 4.0 kB warning / 8.0 kB error |
| `employees.component.css` | 3.25 kB | ~1.0 kB | 4.0 kB warning / 8.0 kB error |
| `reports.component.css` | 3.47 kB | ~1.1 kB | 4.0 kB warning / 8.0 kB error |

**Build Verification**: `Application bundle generation complete. [5.728 seconds]. 0 warnings, 0 errors.` All component stylesheets remain safely within the 4 kB style budget.
