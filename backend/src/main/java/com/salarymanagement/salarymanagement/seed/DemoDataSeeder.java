package com.salarymanagement.salarymanagement.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Service
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String INSERT_COUNTRIES_SQL = """
            INSERT INTO country (code, name, currency_code)
            VALUES ('US', 'United States', 'USD'), ('GB', 'United Kingdom', 'GBP')
            ON CONFLICT (code) DO NOTHING
            """;

    private static final String COUNT_EXISTING_DEMO_SQL = """
            SELECT COUNT(*) FROM employee WHERE employee_number LIKE 'EMP-%'
            """;

    private static final String DELETE_DEMO_COMPENSATIONS_SQL = """
            DELETE FROM compensation_record WHERE employee_id IN (
                SELECT id FROM employee WHERE employee_number LIKE 'EMP-%'
            )
            """;

    private static final String DELETE_DEMO_EMPLOYEES_SQL = """
            DELETE FROM employee WHERE employee_number LIKE 'EMP-%'
            """;

    private static final String INSERT_EMPLOYEE_SQL = """
            INSERT INTO employee (
                id, employee_number, first_name, last_name, email,
                country_code, job_title, department, joined_at, terminated_at, deleted_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_COMPENSATION_SQL = """
            INSERT INTO compensation_record (
                id, employee_id, annual_salary, effective_from, effective_to, created_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final DemoDataGenerator generator;
    private final DemoDataProperties properties;

    public DemoDataSeeder(JdbcTemplate jdbcTemplate, DemoDataGenerator generator, DemoDataProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.generator = generator;
        this.properties = properties;
    }

    @Transactional
    public SeedResult seed() {
        return seed(properties.isReset(), properties.isConfirm());
    }

    @Transactional
    public SeedResult seed(boolean resetRequested, boolean confirmReset) {
        long startTime = System.currentTimeMillis();

        // 1. Ensure supported countries exist
        jdbcTemplate.execute(INSERT_COUNTRIES_SQL);

        // 2. Check for existing demo employees
        Integer existingCount = jdbcTemplate.queryForObject(COUNT_EXISTING_DEMO_SQL, Integer.class);
        if (existingCount == null) {
            existingCount = 0;
        }

        if (resetRequested) {
            if (!confirmReset) {
                throw new IllegalStateException(
                        "Demo data reset requested, but safety confirmation guard is missing. " +
                        "Supply --seed.demo-data.confirm=true to explicitly authorize demo data deletion."
                );
            }
            log.info("Resetting existing demo dataset (deleting records matching 'EMP-%')...");
            jdbcTemplate.execute(DELETE_DEMO_COMPENSATIONS_SQL);
            jdbcTemplate.execute(DELETE_DEMO_EMPLOYEES_SQL);
            log.info("Reset completed. Preparing fresh deterministic seed of {} employees.", properties.getCount());
        } else if (existingCount >= properties.getCount()) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Demo data already seeded ({} demo employees found). Skipping to preserve idempotency.", existingCount);
            return new SeedResult(
                    false,
                    true,
                    0,
                    0,
                    duration,
                    String.format("Demo dataset already present (%d employees). Skipped.", existingCount)
            );
        }

        // 3. Generate deterministic bundles
        log.info("Generating {} deterministic employees and compensation history with random seed {}...",
                properties.getCount(), properties.getRandomSeed());
        List<DemoDataGenerator.GeneratedEmployeeBundle> bundles =
                generator.generate(properties.getCount(), properties.getRandomSeed());

        List<EmployeeSeed> employees = new ArrayList<>(bundles.size());
        List<CompensationSeed> compensations = new ArrayList<>(bundles.size() * 2);

        for (DemoDataGenerator.GeneratedEmployeeBundle bundle : bundles) {
            employees.add(bundle.employee());
            compensations.addAll(bundle.compensations());
        }

        // 4. Batch insert employees
        int batchSize = properties.getBatchSize();
        log.info("Inserting {} employees in batches of {}...", employees.size(), batchSize);
        jdbcTemplate.batchUpdate(
                INSERT_EMPLOYEE_SQL,
                employees,
                batchSize,
                (ps, emp) -> {
                    ps.setObject(1, emp.id());
                    ps.setString(2, emp.employeeNumber());
                    ps.setString(3, emp.firstName());
                    ps.setString(4, emp.lastName());
                    ps.setString(5, emp.email());
                    ps.setString(6, emp.countryCode());
                    ps.setString(7, emp.jobTitle());
                    ps.setString(8, emp.department());
                    ps.setDate(9, Date.valueOf(emp.joinedAt()));
                    if (emp.terminatedAt() != null) {
                        ps.setDate(10, Date.valueOf(emp.terminatedAt()));
                    } else {
                        ps.setNull(10, Types.DATE);
                    }
                    if (emp.deletedAt() != null) {
                        ps.setTimestamp(11, Timestamp.from(emp.deletedAt()));
                    } else {
                        ps.setNull(11, Types.TIMESTAMP_WITH_TIMEZONE);
                    }
                }
        );

        // 5. Batch insert compensation records
        log.info("Inserting {} compensation records in batches of {}...", compensations.size(), batchSize);
        jdbcTemplate.batchUpdate(
                INSERT_COMPENSATION_SQL,
                compensations,
                batchSize,
                (ps, comp) -> {
                    ps.setObject(1, comp.id());
                    ps.setObject(2, comp.employeeId());
                    ps.setBigDecimal(3, comp.annualSalary());
                    ps.setDate(4, Date.valueOf(comp.effectiveFrom()));
                    if (comp.effectiveTo() != null) {
                        ps.setDate(5, Date.valueOf(comp.effectiveTo()));
                    } else {
                        ps.setNull(5, Types.DATE);
                    }
                    ps.setTimestamp(6, Timestamp.from(comp.createdAt()));
                }
        );

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("Demo data seeding completed in {} ms: {} employees and {} compensation records inserted.",
                durationMs, employees.size(), compensations.size());

        return new SeedResult(
                true,
                false,
                employees.size(),
                compensations.size(),
                durationMs,
                String.format("Successfully seeded %d employees and %d compensation records in %d ms.",
                        employees.size(), compensations.size(), durationMs)
        );
    }
}
