# Salary Management System — Domain Design

## 1. Purpose

This document describes the domain model, relationships, business rules, and reporting behavior of the Salary Management System.

The application enables an HR Manager to manage employee records, maintain annual compensation history, and generate salary statistics by country and job title.

The design prioritizes data integrity, clear separation of responsibilities, and future extensibility without implementing payroll or tax functionality in the initial version.

## 2. Domain Model Overview

The initial domain consists of three entities:

1. `Country` — defines supported countries and their configured salary currency.
2. `Employee` — represents an employee record and its employment details.
3. `CompensationRecord` — represents an employee's annual salary for a defined effective period.

### Entity relationships

- One `Country` can have many `Employee` records.
- One `Employee` can have many `CompensationRecord` records.
- Each `Employee` belongs to one `Country`.
- Each `CompensationRecord` belongs to one `Employee`.

A separate `User` entity is not included because authentication, identity management, and rehire workflows are outside the initial scope.

## 3. Entity Definitions

### 3.1 Country

Represents a country supported by the application.

| Field | Type | Constraints / Purpose |
|---|---|---|
| `code` | String | Primary key; country code |
| `name` | String | Required country name |
| `currencyCode` | String | Required ISO 4217 currency code |

**Rules**
- Each country has one configured salary currency.
- Compensation records must use the configured currency for the employee's country under the initial domain model.
- Foreign-exchange conversion is not supported.

### 3.2 Employee

Represents an employee record.

| Field | Type | Constraints / Purpose |
|---|---|---|
| `id` | UUID | Primary key |
| `employeeNumber` | String | Required and unique |
| `firstName` | String | Required |
| `lastName` | String | Required |
| `email` | String | Required and unique |
| `countryCode` | String | Required foreign key to `Country.code` |
| `jobTitle` | String | Required |
| `department` | String | Optional |
| `joinedAt` | Date | Required joining date |
| `terminatedAt` | Date | Nullable termination date |
| `deletedAt` | Timestamp | Nullable soft-deletion timestamp |

**Rules**
- Employee number and email must be unique.
- Joining date is required.
- If a termination date is provided, it must not precede the joining date.
- Termination and deletion are separate concepts.
- Soft deletion sets `deletedAt` instead of physically removing the employee row.
- Deleted employees are excluded from ordinary directory results and current salary reports.
- Rehire and country-transfer workflows are excluded from the initial version.

### 3.3 CompensationRecord

Represents an employee's annual salary for a defined effective period.

| Field | Type | Constraints / Purpose |
|---|---|---|
| `id` | UUID | Primary key |
| `employeeId` | UUID | Required foreign key to `Employee.id` |
| `annualSalary` | Decimal | Required positive annual salary |
| `currencyCode` | String | Required ISO 4217 currency code |
| `effectiveFrom` | Date | Required inclusive start date |
| `effectiveTo` | Date | Nullable exclusive end date |
| `createdAt` | Timestamp | Record creation timestamp |

**Rules**
- Salary must be greater than zero.
- Java `BigDecimal` and a suitable PostgreSQL `NUMERIC` column must be used for salary amounts.
- Compensation is represented as annual salary only.
- `effectiveFrom` is inclusive.
- `effectiveTo` is exclusive.
- A null `effectiveTo` means the period has no scheduled end date.
- Effective periods for the same employee must not overlap.
- At most one compensation record may be effective for an employee on any given date.
- A salary change creates a new compensation record and closes the previous period.
- Historical compensation records must not be overwritten.
- The currency must match the employee's configured country currency under the initial country model.

## 4. Compensation History

Compensation history is maintained using effective-dated records.

For example, suppose an employee's annual salary changes from 60000 to 66000, effective 1 January 2026.

| Annual salary | Effective from | Effective to |
|---:|---|---|
| 60000 | 2025-01-01 | 2026-01-01 |
| 66000 | 2026-01-01 | null |

The previous record ends on 1 January 2026, exclusive. The new record becomes effective on that date.

### Salary change workflow

1. Retrieve the employee and validate the requested salary change.
2. Identify the compensation record effective immediately before the new effective date.
3. Validate the new salary, currency, and effective period.
4. Close the previous record at the new record's effective start date, where applicable.
5. Create the new compensation record.
6. Commit both changes within one database transaction.

The operation must fail if the requested change would create overlapping periods or violate other domain rules.

The service must also handle changes entered with a future effective date without prematurely replacing the employee's current salary.

## 5. Reporting Rules

Reports use the compensation record effective on the requested reporting date. If no date is supplied, the current date is used.

### 5.1 Country salary summary

For a selected country, return:
- Minimum annual salary.
- Maximum annual salary.
- Average annual salary.
- Currency code.
- Number of eligible employees.

### 5.2 Average salary by job title

For a selected country and job title, return:
- Average annual salary.
- Currency code.
- Number of eligible employees.

### Eligibility rules

Current reports exclude:
- Soft-deleted employees.
- Employees whose termination date is on or before the reporting date.
- Employees without a compensation record effective on the reporting date.

A historical compensation record must not be counted as an additional employee. Each eligible employee contributes at most one salary value to a report.

If no eligible employees match the criteria, the application returns a clear no-data result instead of treating the average as zero.

## 6. Employee Directory Behavior

The employee directory supports:
- Text search over relevant employee fields.
- Filtering by country.
- Filtering by job title.
- Sorting by supported fields.
- Pagination with a bounded page size.

Deleted employees are excluded by default. The API validates sort fields and page-size limits rather than accepting arbitrary query expressions.

Directory filters should be implemented in database queries so the application does not load all 10000 employee records into memory for each request.

## 7. Deletion and Data Integrity

Employee deletion is implemented as soft deletion.

When an employee is deleted:
1. Set the `deletedAt` timestamp.
2. Exclude the employee from ordinary directory queries.
3. Exclude the employee from current salary reports.
4. Preserve the employee's compensation history.

Employee number uniqueness remains enforced even for deleted records. This prevents an identifier from being silently reused.

Foreign keys, unique constraints, required fields, and appropriate indexes should be enforced at the database level where applicable. Business rules that span multiple records must also be enforced within transactional service operations.

## 8. Proposed API Boundaries

| Area | Endpoint | Responsibility |
|---|---|---|
| Employee | `GET /api/v1/employees` | Search, filter, sort, and paginate |
| Employee | `POST /api/v1/employees` | Create an employee |
| Employee | `GET /api/v1/employees/{id}` | Retrieve an employee |
| Employee | `PUT /api/v1/employees/{id}` | Update employee details |
| Employee | `DELETE /api/v1/employees/{id}` | Soft-delete an employee |
| Compensation | `GET /api/v1/employees/{id}/compensation` | Retrieve compensation history |
| Compensation | `POST /api/v1/employees/{id}/compensation` | Record a salary change |
| Reporting | `GET /api/v1/reports/country-salary` | Country salary summary |
| Reporting | `GET /api/v1/reports/job-title-salary` | Average salary by country and job title |
| Country | `GET /api/v1/countries` | Retrieve supported country data |

These endpoints define proposed API boundaries. Exact query parameter names, response DTOs, and error formats will be finalized during implementation and documented through OpenAPI.

## 9. Validation and Error Handling

The application validates request data before performing persistence operations.

Examples include:
- Missing required employee fields.
- Duplicate employee number or email.
- Invalid joining or termination dates.
- Non-positive salary.
- Currency mismatch.
- Invalid or overlapping compensation periods.
- Employee or compensation record not found.
- Invalid report filters or pagination parameters.

Use consistent HTTP status codes and a structured error response that provides a useful message without exposing internal implementation details.

## 10. Future Extension Points

The initial version excludes payroll execution, tax calculations, deductions, bonuses, bank transfers, foreign-exchange conversion, rehire workflows, and country transfers.

Future payroll and tax functionality should be introduced as separate modules that consume employee and compensation data. Those features must not be embedded into employee CRUD logic.

The initial design deliberately avoids implementing speculative entities or infrastructure for excluded features.

## 11. Design Priorities

The design prioritizes:
- Accurate compensation history.
- Correct salary calculations and currency handling.
- Database integrity and transactional updates.
- Maintainable business module boundaries.
- Efficient employee search and reporting queries.
- Deterministic seed data and meaningful automated tests.
- Straightforward deployment and demonstration.