package com.salarymanagement.salarymanagement.seed;

import com.salarymanagement.salarymanagement.dto.response.CountryJobTitleSalaryResponse;
import com.salarymanagement.salarymanagement.dto.response.CountrySalarySummaryResponse;
import com.salarymanagement.salarymanagement.service.ReportingService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoDataSeederTests {

    @Autowired
    private DemoDataGenerator generator;

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private DemoDataProperties properties;

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterAll
    void tearDown() {
        // Clean up demo data after test class finishes to leave test database clean
        jdbcTemplate.execute("DELETE FROM compensation_record WHERE employee_id IN (SELECT id FROM employee WHERE employee_number LIKE 'EMP-%')");
        jdbcTemplate.execute("DELETE FROM employee WHERE employee_number LIKE 'EMP-%'");
    }

    @Test
    @Order(1)
    @DisplayName("Generator should be 100% deterministic, unique, and obey business rules")
    void shouldGenerateDeterministicEmployeesAndValidHistory() {
        long fixedSeed = 20261015L;
        int count = 500;

        List<DemoDataGenerator.GeneratedEmployeeBundle> run1 = generator.generate(count, fixedSeed);
        List<DemoDataGenerator.GeneratedEmployeeBundle> run2 = generator.generate(count, fixedSeed);

        assertEquals(count, run1.size());
        assertEquals(count, run2.size());

        Set<String> employeeNumbers = new HashSet<>();
        Set<String> emails = new HashSet<>();

        for (int i = 0; i < count; i++) {
            DemoDataGenerator.GeneratedEmployeeBundle b1 = run1.get(i);
            DemoDataGenerator.GeneratedEmployeeBundle b2 = run2.get(i);

            // Determinism check
            assertEquals(b1.employee().id(), b2.employee().id());
            assertEquals(b1.employee().employeeNumber(), b2.employee().employeeNumber());
            assertEquals(b1.employee().email(), b2.employee().email());
            assertEquals(b1.employee().firstName(), b2.employee().firstName());
            assertEquals(b1.employee().lastName(), b2.employee().lastName());
            assertEquals(b1.employee().countryCode(), b2.employee().countryCode());
            assertEquals(b1.employee().jobTitle(), b2.employee().jobTitle());
            assertEquals(b1.employee().department(), b2.employee().department());
            assertEquals(b1.employee().joinedAt(), b2.employee().joinedAt());
            assertEquals(b1.employee().terminatedAt(), b2.employee().terminatedAt());
            assertEquals(b1.employee().deletedAt(), b2.employee().deletedAt());
            assertEquals(b1.compensations().size(), b2.compensations().size());

            for (int c = 0; c < b1.compensations().size(); c++) {
                CompensationSeed c1 = b1.compensations().get(c);
                CompensationSeed c2 = b2.compensations().get(c);
                assertEquals(c1.id(), c2.id());
                assertEquals(c1.annualSalary(), c2.annualSalary());
                assertEquals(c1.effectiveFrom(), c2.effectiveFrom());
                assertEquals(c1.effectiveTo(), c2.effectiveTo());
            }

            // Uniqueness check
            assertTrue(employeeNumbers.add(b1.employee().employeeNumber()), "Duplicate employee number: " + b1.employee().employeeNumber());
            assertTrue(emails.add(b1.employee().email()), "Duplicate email: " + b1.employee().email());

            // Employment date constraints
            if (b1.employee().terminatedAt() != null) {
                assertTrue(b1.employee().terminatedAt().isAfter(b1.employee().joinedAt()),
                        "Terminated at must be after joined at");
            }

            // Compensation rules check
            List<CompensationSeed> comps = b1.compensations();
            assertFalse(comps.isEmpty(), "Employee must have at least one compensation record");

            long openEndedCount = comps.stream().filter(c -> c.effectiveTo() == null).count();
            if (b1.employee().terminatedAt() != null) {
                assertEquals(0, openEndedCount, "Terminated employee must have no open-ended compensation records");
                CompensationSeed lastComp = comps.get(comps.size() - 1);
                assertEquals(b1.employee().terminatedAt(), lastComp.effectiveTo(),
                        "Last compensation record must end on termination date");
            } else {
                assertEquals(1, openEndedCount, "Active employee must have exactly one open-ended compensation record");
            }

            for (int k = 0; k < comps.size(); k++) {
                CompensationSeed current = comps.get(k);
                assertTrue(current.annualSalary().compareTo(BigDecimal.ZERO) > 0, "Salary must be positive");
                assertFalse(current.effectiveFrom().isBefore(b1.employee().joinedAt()),
                        "effectiveFrom cannot be before employee join date");

                if (current.effectiveTo() != null) {
                    assertTrue(current.effectiveTo().isAfter(current.effectiveFrom()),
                            "effectiveTo must be strictly after effectiveFrom");
                }

                if (k > 0) {
                    CompensationSeed prev = comps.get(k - 1);
                    assertEquals(prev.effectiveTo(), current.effectiveFrom(),
                            "Contiguous compensation records must match boundary dates exactly");
                }
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should seed exactly 10,000 employees and realistic compensations into PostgreSQL")
    void shouldSeed10000EmployeesIntoDatabase() {
        // Run seed
        SeedResult result = seeder.seed();

        assertTrue(result.executed());
        assertFalse(result.skipped());
        assertEquals(10_000, result.employeesInserted());
        assertTrue(result.compensationsInserted() >= 10_000, "Should insert at least 10,000 compensation records");
        assertTrue(result.durationMs() > 0, "Duration must be recorded");

        // Verify counts directly via SQL
        Integer employeeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE employee_number LIKE 'EMP-%'", Integer.class);
        assertEquals(10_000, employeeCount);

        Integer compCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compensation_record WHERE employee_id IN (SELECT id FROM employee WHERE employee_number LIKE 'EMP-%')",
                Integer.class);
        assertNotNull(compCount);
        assertEquals(result.compensationsInserted(), compCount);

        // Verify country breakdown
        Integer usCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE country_code = 'US' AND employee_number LIKE 'EMP-%'", Integer.class);
        Integer gbCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE country_code = 'GB' AND employee_number LIKE 'EMP-%'", Integer.class);

        assertNotNull(usCount);
        assertNotNull(gbCount);
        assertEquals(10_000, usCount + gbCount);
        assertTrue(usCount > 5000, "US should be ~60% of employees");
        assertTrue(gbCount > 3000, "GB should be ~40% of employees");
    }

    @Test
    @Order(3)
    @DisplayName("Second seed call should be idempotent and skip insertion when already present")
    void shouldBeIdempotentOnSecondRun() {
        SeedResult secondRun = seeder.seed(false, false);

        assertFalse(secondRun.executed(), "Should not re-execute");
        assertTrue(secondRun.skipped(), "Should be skipped");
        assertEquals(0, secondRun.employeesInserted());
        assertEquals(0, secondRun.compensationsInserted());

        Integer employeeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE employee_number LIKE 'EMP-%'", Integer.class);
        assertEquals(10_000, employeeCount);
    }

    @Test
    @Order(4)
    @DisplayName("Resetting without explicit confirmation guard must fail with IllegalStateException")
    void shouldRejectResetWithoutConfirmationGuard() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> seeder.seed(true, false));
        assertTrue(ex.getMessage().contains("confirmation guard is missing"));

        // Verify data was NOT deleted
        Integer employeeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE employee_number LIKE 'EMP-%'", Integer.class);
        assertEquals(10_000, employeeCount);
    }

    @Test
    @Order(5)
    @DisplayName("Reports should return sensible non-empty metrics for seeded data")
    void shouldProduceSensibleReportingResults() {
        LocalDate asOfDate = LocalDate.of(2026, 10, 15);

        // US Salary Summary
        CountrySalarySummaryResponse usSummary = reportingService.getCountrySalarySummary("US", asOfDate);
        assertNotNull(usSummary);
        assertEquals("US", usSummary.getCountryCode());
        assertEquals("USD", usSummary.getCurrencyCode());
        assertTrue(usSummary.getEligibleEmployeeCount() > 4000, "Should have thousands of eligible US employees");
        assertNotNull(usSummary.getMinSalary());
        assertNotNull(usSummary.getMaxSalary());
        assertNotNull(usSummary.getAverageSalary());
        assertTrue(usSummary.getMinSalary().compareTo(usSummary.getAverageSalary()) <= 0);
        assertTrue(usSummary.getAverageSalary().compareTo(usSummary.getMaxSalary()) <= 0);

        // GB Salary Summary
        CountrySalarySummaryResponse gbSummary = reportingService.getCountrySalarySummary("GB", asOfDate);
        assertNotNull(gbSummary);
        assertEquals("GB", gbSummary.getCountryCode());
        assertEquals("GBP", gbSummary.getCurrencyCode());
        assertTrue(gbSummary.getEligibleEmployeeCount() > 2500, "Should have thousands of eligible GB employees");
        assertNotNull(gbSummary.getMinSalary());
        assertNotNull(gbSummary.getMaxSalary());
        assertNotNull(gbSummary.getAverageSalary());

        // US Job Title Breakdown
        CountryJobTitleSalaryResponse usJobTitles = reportingService.getSalaryByJobTitle("US", asOfDate, null);
        assertNotNull(usJobTitles);
        assertFalse(usJobTitles.getJobTitles().isEmpty(), "Should return job title salary breakdown");
        assertTrue(usJobTitles.getTotalEligibleEmployees() > 4000);

        // Filtered Job Title Breakdown
        CountryJobTitleSalaryResponse softwareEngUS = reportingService.getSalaryByJobTitle("US", asOfDate, "Software Engineer");
        assertNotNull(softwareEngUS);
        assertEquals(1, softwareEngUS.getJobTitles().size());
        assertEquals("Software Engineer", softwareEngUS.getJobTitles().get(0).getJobTitle());
        assertTrue(softwareEngUS.getJobTitles().get(0).getEmployeeCount() > 0);
        assertNotNull(softwareEngUS.getJobTitles().get(0).getAverageSalary());
    }

    @Test
    @Order(6)
    @DisplayName("Resetting with explicit confirmation guard should cleanly re-seed")
    void shouldAllowResetAndReSeedWhenConfirmed() {
        SeedResult resetResult = seeder.seed(true, true);

        assertTrue(resetResult.executed());
        assertFalse(resetResult.skipped());
        assertEquals(10_000, resetResult.employeesInserted());

        Integer employeeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE employee_number LIKE 'EMP-%'", Integer.class);
        assertEquals(10_000, employeeCount);
    }
}
