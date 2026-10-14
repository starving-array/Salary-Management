# Salary Management System — Requirements

## 1. Goal

Build a web-based salary management application that replaces spreadsheet-based workflows and enables an HR Manager to maintain employee records, track annual compensation changes, and review salary statistics across countries and job titles.

## 2. Primary User

**HR Manager** — manages employee information, maintains compensation history, and reviews salary reports.

## 3. Functional Requirements

### Employee Management
- Create, view, update, and delete employee records.
- Maintain employee number, name, email, country, job title, optional department, joining date, and optional termination date.
- Provide employee search, country and job-title filters, sorting, and pagination.

### Compensation Management
- Record annual salary, currency, and effective dates.
- Preserve historical compensation records when salaries change instead of overwriting previous salary values.
- Validate positive salary amounts, country-currency consistency, and non-overlapping effective periods.
- Display an employee's current and historical compensation records.

### Salary Reports
- Calculate minimum, maximum, and average annual salary for a selected country.
- Calculate average annual salary for a specified job title within a country.
- Display the reporting currency and number of employees included.
- Exclude deleted and terminated employees from current salary reports.
- Handle cases with no eligible employees without presenting zero as a real salary statistic.

### Sample Data
- Provide a repeatable, reasonably fast seed process that creates 10,000 employees with realistic fictional data and compensation records.
- Use deterministic data generation so repeated runs reproduce the intended dataset.
- Restrict destructive seed-reset operations to a dedicated development/demo environment; never run them automatically against production data.

## 4. Non-Functional Requirements

- Use a maintainable, modular backend and frontend architecture.
- Enforce data integrity through validation, relational constraints, and appropriate database indexes.
- Provide meaningful, deterministic unit tests for core business rules, plus integration tests for persistence and reporting behavior.
- Support efficient queries and paginated employee listings for the 10,000-employee dataset.
- Include setup instructions, design and architecture documentation, trade-offs, performance considerations, and AI usage notes.
- Deploy a functional application and provide a video demonstration.

## 5. Scope Exclusions

The initial version will not implement:
- Authentication or authorization.
- Payroll execution or payslip generation.
- Tax calculations, deductions, or bonuses.
- Bank transfers or payment processing.
- Foreign-exchange conversion.
- Employee rehire workflows.
- Country-transfer workflows.

These features are excluded to keep the assessment focused. The employee and compensation modules should have clear boundaries so future payroll and tax capabilities can be introduced without coupling those rules to employee maintenance.

## 6. Domain Rules and Reporting Assumptions

- Each country has one configured salary currency; salary reports do not combine different currencies or perform foreign-exchange conversion.
- Compensation is represented as annual salary only.
- Compensation effective start dates are inclusive and effective end dates are exclusive. A null end date means no scheduled end date.
- An employee must not have overlapping compensation periods.
- Salary changes create new compensation records, preserving previous records.
- Current salary reports use the compensation record effective on the report date, defaulting to the current date.
- Deleted and terminated employees are excluded from current salary reports, while historical records are retained.
- Each report identifies its currency and employee count.

## 7. Acceptance Criteria

- Employee CRUD operations work correctly.
- The employee directory supports search, country and job-title filters, sorting, and pagination.
- Compensation changes preserve history and reject invalid salary values and overlapping effective periods.
- The country report returns correct minimum, maximum, and average annual salary.
- The job-title report returns the correct average annual salary for the selected country and job title.
- Reports select only the compensation record effective on the requested date and do not count historical records as additional employees.
- The seed process safely reproduces the intended 10,000-employee dataset in a dedicated development/demo environment.
- Automated tests cover core validation, compensation history, and reporting cases.
- The application runs from documented instructions, is accessible through a deployed URL, and is demonstrated in a video.
