# Salary Management System — Architecture

## 1. Purpose

This document describes the technical architecture of the Salary Management System assessment.

The application enables an HR Manager to manage employee records, maintain annual compensation history, and generate salary statistics by country and job title.

The architecture prioritizes correctness, maintainability, testability, and straightforward deployment. It uses a modular monolith rather than distributed services, avoiding unnecessary infrastructure while preserving clear business boundaries.

## 2. Technology Stack

| Component | Technology | Purpose |
|---|---|---|
| Backend language | Java 21 | Backend implementation |
| Backend framework | Spring Boot | REST APIs and application configuration |
| Build system | Gradle Wrapper | Reproducible builds and dependency management |
| Persistence | Spring Data JPA / Hibernate | Relational data access |
| Database | PostgreSQL | Persistent relational storage |
| Schema migrations | Flyway | Version-controlled database changes |
| Frontend | Angular with TypeScript | HR management interface |
| Frontend tooling | Angular CLI | Frontend development and builds |
| Backend testing | JUnit 5, Mockito, Spring Boot Test | Unit and integration testing |
| Frontend testing | Angular testing tools | Component and service testing |
| Local infrastructure | Docker Compose | Local database environment |
| API communication | REST and JSON | Frontend/backend integration |
| API documentation | OpenAPI | API contract documentation |

Use compatible stable dependency versions and commit the Gradle Wrapper. The Java toolchain, Gradle configuration, and IDE settings must consistently target Java 21.

## 3. High-Level Architecture

The system contains three primary layers:

1. **Frontend:** Angular application for employee management, compensation history, and salary reports.
2. **Backend:** Spring Boot REST API responsible for validation, business rules, reporting, and persistence.
3. **Database:** PostgreSQL for employee records, compensation history, and country reference data.

### Request flow

Angular UI → REST Controller → Application Service → Repository → PostgreSQL

Controllers handle HTTP requests and responses. Services implement business operations and enforce domain rules. Repositories provide database access. DTOs separate the API contract from persistence entities.

The frontend and backend can be developed and tested independently while communicating through a documented REST API.

## 4. Backend Module Structure

The backend will be a single Spring Boot application organized into business-focused modules.

### Employee module
- Create, retrieve, update, and soft-delete employees.
- Search employee records.
- Filter by country and job title.
- Support sorting and pagination.
- Validate employee information and employment dates.

### Compensation module
- Record annual salary and currency.
- Maintain effective-dated compensation history.
- Validate salary amounts and effective periods.
- Retrieve current and historical compensation records.

### Reporting module
- Calculate minimum, maximum, and average annual salary by country.
- Calculate average annual salary by job title within a country.
- Exclude deleted and terminated employees from current reports.
- Include currency and employee count in report results.

### Country module
- Maintain supported country reference data.
- Define the configured salary currency for each country.
- Supply country data to frontend forms and filters.

### Common module
- Shared exception handling.
- Request validation conventions.
- Consistent API error responses.
- Shared configuration and cross-cutting infrastructure concerns.

These modules remain within one deployable application. Microservices, message brokers, distributed transactions, and caching infrastructure are not required for the initial scope.

## 5. Domain and Data Architecture

The domain consists of three entities: `Country`, `Employee`, and `CompensationRecord`.

### Country
- `code`: primary key using a country code.
- `name`: required country name.
- `currencyCode`: configured ISO 4217 currency code.

### Employee
- `id`: UUID primary key.
- `employeeNumber`: required and unique.
- `firstName` and `lastName`: required.
- `email`: required and unique.
- `countryCode`: foreign key to Country.
- `jobTitle`: required.
- `department`: optional.
- `joinedAt`: required joining date.
- `terminatedAt`: nullable termination date.
- `deletedAt`: nullable timestamp for soft deletion.

### CompensationRecord
- `id`: UUID primary key.
- `employeeId`: foreign key to Employee.
- `annualSalary`: positive decimal amount.
- `currencyCode`: ISO 4217 currency code.
- `effectiveFrom`: inclusive start date.
- `effectiveTo`: nullable exclusive end date.
- `createdAt`: record creation timestamp.

### Domain rules
- Use Java `BigDecimal` and PostgreSQL `NUMERIC` for salary values.
- Reject zero or negative salaries.
- Prevent overlapping compensation periods for the same employee.
- Allow at most one compensation record to be effective for an employee on any given date.
- Record salary changes as new compensation records rather than overwriting historical values.
- Enforce country-currency consistency.
- Treat termination and deletion as separate concepts.
- Retain historical records when an employee is soft-deleted.
- Exclude rehire and country-transfer workflows from the initial version.

## 6. Reporting Architecture

Reports use the compensation record effective on the requested reporting date. The current date is the default when no date is supplied.

### Country salary report
Returns minimum, maximum, and average annual salary for eligible employees in a selected country.

### Job-title salary report
Returns average annual salary for eligible employees matching a selected country and job title.

Each report includes the reporting currency and number of eligible employees.

The reporting layer must select only the compensation record effective on the reporting date. Historical records must not cause an employee to be counted multiple times.

Deleted employees, terminated employees, and employees without an effective compensation record are excluded from current reports. An empty result must be represented clearly rather than treating zero as a real salary statistic.

## 7. REST API Design

All endpoints use the `/api/v1` namespace.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/employees` | Search, filter, sort, and paginate employees |
| POST | `/api/v1/employees` | Create employee |
| GET | `/api/v1/employees/{id}` | Retrieve employee |
| PUT | `/api/v1/employees/{id}` | Update employee |
| DELETE | `/api/v1/employees/{id}` | Soft-delete employee |
| GET | `/api/v1/employees/{id}/compensation` | Retrieve compensation history |
| POST | `/api/v1/employees/{id}/compensation` | Record a salary change |
| GET | `/api/v1/reports/country-salary` | Retrieve country salary statistics |
| GET | `/api/v1/reports/job-title-salary` | Retrieve average salary by country and job title |
| GET | `/api/v1/countries` | Retrieve supported countries |

Use request and response DTOs rather than exposing JPA entities directly. Apply Bean Validation, consistent error responses, and bounded pagination. Validate sort fields against an allowlist.

OpenAPI will document endpoints, parameters, request bodies, responses, and validation errors.

## 8. Database and Schema Management

PostgreSQL is the primary database for local development and deployment.

Flyway migrations define and evolve the database schema. Migrations are version-controlled and committed alongside application changes.

Use database constraints for required fields, unique employee identifiers, unique email addresses, and foreign-key relationships. Add indexes for actual access patterns, including employee directory filters and compensation effective-date lookups.

Compensation changes that close a previous record and create a new record must execute in one database transaction.

## 9. Seed Data Strategy

The assessment requires 10,000 employees with realistic fictional data.

The seed process will:
- Generate deterministic data using a fixed seed.
- Create consistent employee, country, and compensation records.
- Produce the intended dataset on repeated runs.
- Run reasonably quickly and report record counts.
- Be invoked explicitly in a development/demo environment.

Destructive reset operations must be guarded by an explicit environment or configuration check. They must never run automatically at application startup or operate against production data.

The seed process should verify record counts and important data invariants after completion.

## 10. Testing Strategy

### Unit tests
Use JUnit 5 and Mockito to test employee validation, salary validation, effective-date rules, compensation changes, and reporting logic.

### Persistence and integration tests
Verify database constraints, compensation history, soft deletion, filtering, pagination, and salary report queries against PostgreSQL.

### Frontend tests
Test Angular components, form validation, API services, and report rendering.

### End-to-end smoke tests
Verify the main demonstration workflow:
1. Create and update an employee.
2. View employee details.
3. Record a compensation change.
4. Verify compensation history.
5. Retrieve both required salary reports.

Tests should be deterministic, understandable, and efficient enough for regular development.

## 11. Security and Scope Boundaries

Authentication and authorization are excluded from the assessment version. The deployed application must use fictional demonstration data and must not be presented as a secure production HR system.

The initial version also excludes payroll execution, tax calculations, deductions, bonuses, bank transfers, foreign-exchange conversion, employee rehire workflows, and country-transfer workflows.

The compensation module remains separate from employee maintenance so future payroll and tax modules can be introduced without embedding those rules into employee CRUD operations.

## 12. Deployment Architecture

Deploy the Angular frontend, Spring Boot backend, and PostgreSQL database using compatible hosting services.

Before choosing providers, verify:
- Database persistence and retention policies.
- Application sleep and restart behavior.
- Database connection limits.
- Supported Java runtime and environment configuration.
- Frontend-to-backend connectivity and CORS configuration.
- Secret management and environment-specific configuration.

The final delivery must include a working public application URL, local setup instructions, migration and seed instructions, test commands, and a video demonstration.

## 13. Project Structure

```text
salary-management/
├── backend/
│   ├── src/main/java/.../
│   │   ├── employee/
│   │   ├── compensation/
│   │   ├── reporting/
│   │   ├── country/
│   │   └── common/
│   ├── src/main/resources/db/migration/
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradlew
├── frontend/
│   ├── src/app/
│   │   ├── employees/
│   │   ├── compensation/
│   │   ├── reports/
│   │   ├── countries/
│   │   └── shared/
│   └── angular.json
├── docs/
│   ├── requirements.md
│   ├── design.md
│   ├── architecture.md
│   ├── trade-offs.md
│   ├── performance.md
│   └── ai-usage.md
├── docker-compose.yml
└── README.md
```

The exact package and frontend folder names may be adjusted during implementation, provided the business boundaries remain clear.

## 14. Delivery Artifacts

The final repository will include:
- Requirements and domain design.
- Architecture and trade-off documentation.
- Performance findings based on actual measurements.
- AI usage notes and relevant prompts.
- Reproducible database migrations and seed instructions.
- Automated tests and local setup instructions.
- Deployment URL and video demonstration.