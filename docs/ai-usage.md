# AI Usage & Engineering Methodology Notes

This document summarizes the use of AI tools (Antigravity AI Pair Programming Assistant) during the design, implementation, and verification of the Salary Management System assessment.

---

## 1. Overview of AI Assistance

AI was utilized as an advanced pair programmer to accelerate implementation speed, maintain consistency across layers, and write exhaustive unit and integration tests. All architectural decisions, domain constraints, and verification steps were actively validated against the project requirements.

---

## 2. Key Areas Where AI Assisted

1. **Test Suite Generation**:
   - Scaffolding comprehensive JUnit 5 and Vitest test suites.
   - Creating boundary condition tests for date overlapping, salary validations, and HTTP error mappings.
   - Generating mock datasets for frontend component unit tests.

2. **Frontend UI Implementation**:
   - Building standalone Angular components adhering to modern reactive patterns (`inject()`, `FormBuilder`, `takeUntilDestroyed()`).
   - Maintaining accessibility (`aria-label`, `aria-live`, semantic table headers).
   - Optimizing component CSS to remain strictly under Angular CLI's 4 kB style budget.

3. **Deterministic Seeding Logic**:
   - Generating realistic dictionaries of names, job titles, and salary ranges.
   - Formulating JDBC batch update syntax for high-performance PostgreSQL bulk inserts.

---

## 3. Engineering Decisions & Critical Review

While AI assisted in code generation, human engineering direction was essential in key architectural areas:

- **Effective-Date Invariants**: Formulating exact inclusive/exclusive boundary checks (`[effectiveFrom, effectiveTo)`) and ensuring `effectiveTo` in terminated employee compensation records terminates precisely on `terminatedAt`.
- **Database Partial Indexing**: Designing the PostgreSQL partial unique index (`uq_compensation_open_ended`) to enforce single active salaries without preventing multiple historical closed records.
- **Safety Guards**: Introducing the mandatory `--seed.demo-data.confirm=true` guard to prevent destructive resets during development or demo execution.
- **Zoneless / Vitest Compatibility**: Correcting timer simulation from Angular's `fakeAsync()` (which requires `zone.js`) to native Vitest fake timers (`vi.useFakeTimers()` / `vi.advanceTimersByTime()`).
- **Empirical Validation**: Requiring all performance numbers, test counts, and durations to be gathered from real test runs rather than accepting assumed estimates.

---

## 4. Verification Workflow

Every prompt and phase followed a strict verification cycle:
1. Verify baseline git branch and working tree.
2. Read backend and frontend source files and existing contracts before making changes.
3. Apply targeted, small changes without touching unrelated files or sibling directories.
4. Execute automated backend test suite (`./gradlew test`) and frontend test suite (`npm test -- --no-watch`).
5. Verify production build (`npm run build`) and budget compliance.
6. Commit with focused, descriptive semantic commit messages.
