package com.salarymanagement.salarymanagement.controller;

import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.repository.CompensationRecordRepository;
import com.salarymanagement.salarymanagement.repository.CountryRepository;
import com.salarymanagement.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportingControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CompensationRecordRepository compensationRecordRepository;

    private Country countryUS;
    private Country countryGB;

    @BeforeEach
    void setUp() {
        countryUS = countryRepository.findById("TU")
                .orElseGet(() -> countryRepository.save(new Country("TU", "Test United States", "USD")));
        countryGB = countryRepository.findById("TG")
                .orElseGet(() -> countryRepository.save(new Country("TG", "Test United Kingdom", "GBP")));
    }

    private Employee createEmployee(String empNumber, String email, Country country, String jobTitle,
                                    LocalDate joinedAt, LocalDate terminatedAt, Instant deletedAt) {
        Employee employee = new Employee(
                UUID.randomUUID(),
                empNumber,
                "First",
                "Last",
                email,
                country,
                jobTitle,
                "Engineering",
                joinedAt,
                terminatedAt,
                deletedAt
        );
        return employeeRepository.save(employee);
    }

    private CompensationRecord createCompensation(Employee employee, BigDecimal salary,
                                                  LocalDate effectiveFrom, LocalDate effectiveTo) {
        CompensationRecord record = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                salary,
                effectiveFrom,
                effectiveTo,
                Instant.now()
        );
        return compensationRecordRepository.save(record);
    }

    @Test
    @DisplayName("should calculate accurate min, max, and average salary for eligible employees in country")
    void shouldReturnAccurateCountrySalarySummary() throws Exception {
        Employee e1 = createEmployee("REP_01", "rep1@example.com", countryUS, "Engineer", LocalDate.of(2025, 1, 1), null, null);
        Employee e2 = createEmployee("REP_02", "rep2@example.com", countryUS, "Architect", LocalDate.of(2025, 1, 1), null, null);
        Employee e3 = createEmployee("REP_03", "rep3@example.com", countryUS, "Director", LocalDate.of(2025, 1, 1), null, null);

        createCompensation(e1, new BigDecimal("60000.00"), LocalDate.of(2025, 1, 1), null);
        createCompensation(e2, new BigDecimal("80000.00"), LocalDate.of(2025, 1, 1), null);
        createCompensation(e3, new BigDecimal("100000.00"), LocalDate.of(2025, 1, 1), null);

        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-06-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("TU"))
                .andExpect(jsonPath("$.countryName").value("Test United States"))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.asOfDate").value("2025-06-01"))
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(3))
                .andExpect(jsonPath("$.minSalary").value(60000.00))
                .andExpect(jsonPath("$.maxSalary").value(100000.00))
                .andExpect(jsonPath("$.averageSalary").value(80000.00));
    }

    @Test
    @DisplayName("should reflect historical salary changes accurately before and after change date")
    void shouldReflectHistoricalSalaryChangesAccurately() throws Exception {
        Employee e1 = createEmployee("REP_HIST_01", "rephist1@example.com", countryUS, "Engineer", LocalDate.of(2024, 1, 1), null, null);

        createCompensation(e1, new BigDecimal("50000.00"), LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1));
        createCompensation(e1, new BigDecimal("70000.00"), LocalDate.of(2025, 1, 1), null);

        // Before salary change (as of 2024-06-01)
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2024-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(1))
                .andExpect(jsonPath("$.minSalary").value(50000.00))
                .andExpect(jsonPath("$.maxSalary").value(50000.00))
                .andExpect(jsonPath("$.averageSalary").value(50000.00));

        // After salary change (as of 2025-06-01)
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(1))
                .andExpect(jsonPath("$.minSalary").value(70000.00))
                .andExpect(jsonPath("$.maxSalary").value(70000.00))
                .andExpect(jsonPath("$.averageSalary").value(70000.00));
    }

    @Test
    @DisplayName("should filter salary summary by job title when jobTitle param is provided")
    void shouldFilterSalarySummaryByJobTitle() throws Exception {
        Employee e1 = createEmployee("REP_JT_01", "repjt1@example.com", countryUS, "Software Engineer", LocalDate.of(2024, 1, 1), null, null);
        Employee e2 = createEmployee("REP_JT_02", "repjt2@example.com", countryUS, "Software Engineer", LocalDate.of(2024, 1, 1), null, null);
        Employee e3 = createEmployee("REP_JT_03", "repjt3@example.com", countryUS, "Product Manager", LocalDate.of(2024, 1, 1), null, null);

        createCompensation(e1, new BigDecimal("80000.00"), LocalDate.of(2024, 1, 1), null);
        createCompensation(e2, new BigDecimal("120000.00"), LocalDate.of(2024, 1, 1), null);
        createCompensation(e3, new BigDecimal("150000.00"), LocalDate.of(2024, 1, 1), null);

        // Filtered by Software Engineer
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-01-01")
                        .param("jobTitle", "Software Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(2))
                .andExpect(jsonPath("$.minSalary").value(80000.00))
                .andExpect(jsonPath("$.maxSalary").value(120000.00))
                .andExpect(jsonPath("$.averageSalary").value(100000.00));

        // Unfiltered (country-wide)
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(3))
                .andExpect(jsonPath("$.minSalary").value(80000.00))
                .andExpect(jsonPath("$.maxSalary").value(150000.00))
                .andExpect(jsonPath("$.averageSalary").value(116666.67));
    }

    @Test
    @DisplayName("should respect effective date boundaries with inclusive start and exclusive end")
    void shouldRespectEffectiveDateBoundaries() throws Exception {
        Employee e1 = createEmployee("REP_BOUND_01", "repbound@example.com", countryUS, "Engineer", LocalDate.of(2025, 1, 1), null, null);

        createCompensation(e1, new BigDecimal("50000.00"), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 1));
        createCompensation(e1, new BigDecimal("65000.00"), LocalDate.of(2025, 7, 1), null);

        // On 2025-07-01: period 1 has exclusive end (excluded), period 2 has inclusive start (included)
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(1))
                .andExpect(jsonPath("$.averageSalary").value(65000.00));
    }

    @Test
    @DisplayName("should exclude soft-deleted and terminated employees from report")
    void shouldExcludeSoftDeletedAndTerminatedEmployees() throws Exception {
        // Active employee
        Employee active = createEmployee("REP_ACTIVE", "active@example.com", countryUS, "Dev", LocalDate.of(2025, 1, 1), null, null);
        createCompensation(active, new BigDecimal("60000.00"), LocalDate.of(2025, 1, 1), null);

        // Terminated employee (terminated on 2025-05-01)
        Employee terminated = createEmployee("REP_TERM", "term@example.com", countryUS, "Dev", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 5, 1), null);
        createCompensation(terminated, new BigDecimal("80000.00"), LocalDate.of(2025, 1, 1), null);

        // Soft-deleted employee
        Employee deleted = createEmployee("REP_DEL", "del@example.com", countryUS, "Dev", LocalDate.of(2025, 1, 1), null, Instant.now());
        createCompensation(deleted, new BigDecimal("90000.00"), LocalDate.of(2025, 1, 1), null);

        // On 2025-06-01: terminated on 2025-05-01 is excluded, deleted is excluded -> only active
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(1))
                .andExpect(jsonPath("$.averageSalary").value(60000.00));

        // On 2025-04-01: terminatedAt (2025-05-01) is in future -> both active and terminated are eligible
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-04-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(2))
                .andExpect(jsonPath("$.minSalary").value(60000.00))
                .andExpect(jsonPath("$.maxSalary").value(80000.00))
                .andExpect(jsonPath("$.averageSalary").value(70000.00));
    }

    @Test
    @DisplayName("should not combine currencies across different countries")
    void shouldNotCombineCurrenciesAcrossCountries() throws Exception {
        Employee usEmp = createEmployee("REP_US", "us@example.com", countryUS, "Dev", LocalDate.of(2025, 1, 1), null, null);
        createCompensation(usEmp, new BigDecimal("100000.00"), LocalDate.of(2025, 1, 1), null);

        Employee gbEmp = createEmployee("REP_GB", "gb@example.com", countryGB, "Dev", LocalDate.of(2025, 1, 1), null, null);
        createCompensation(gbEmp, new BigDecimal("60000.00"), LocalDate.of(2025, 1, 1), null);

        // US report
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("TU"))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(1))
                .andExpect(jsonPath("$.averageSalary").value(100000.00));

        // GB report
        mockMvc.perform(get("/api/v1/reports/countries/TG/salary-summary")
                        .param("asOfDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("TG"))
                .andExpect(jsonPath("$.currencyCode").value("GBP"))
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(1))
                .andExpect(jsonPath("$.averageSalary").value(60000.00));
    }

    @Test
    @DisplayName("should return clear no-data result with null statistics when country has no eligible employees")
    void shouldReturnClearNoDataResultWhenCountryHasNoEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("TU"))
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(0))
                .andExpect(jsonPath("$.minSalary").value(nullValue()))
                .andExpect(jsonPath("$.maxSalary").value(nullValue()))
                .andExpect(jsonPath("$.averageSalary").value(nullValue()));
    }

    @Test
    @DisplayName("should return 404 Not Found when country code does not exist")
    void shouldReturnNotFoundForUnknownCountry() throws Exception {
        mockMvc.perform(get("/api/v1/reports/countries/UNKNOWN/salary-summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/v1/reports/countries/UNKNOWN/salary-by-job-title"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("should return 400 Bad Request when asOfDate is malformed")
    void shouldReturnBadRequestForMalformedDate() throws Exception {
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary")
                        .param("asOfDate", "not-a-valid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("should return grouped salaries by job title sorted alphabetically with counts")
    void shouldReturnSalariesGroupedByJobTitle() throws Exception {
        Employee e1 = createEmployee("REP_JT_01", "jt1@example.com", countryUS, "Senior Engineer", LocalDate.of(2025, 1, 1), null, null);
        Employee e2 = createEmployee("REP_JT_02", "jt2@example.com", countryUS, "Senior Engineer", LocalDate.of(2025, 1, 1), null, null);
        Employee e3 = createEmployee("REP_JT_03", "jt3@example.com", countryUS, "Junior Engineer", LocalDate.of(2025, 1, 1), null, null);

        createCompensation(e1, new BigDecimal("90000.00"), LocalDate.of(2025, 1, 1), null);
        createCompensation(e2, new BigDecimal("110000.00"), LocalDate.of(2025, 1, 1), null);
        createCompensation(e3, new BigDecimal("50000.00"), LocalDate.of(2025, 1, 1), null);

        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-by-job-title")
                        .param("asOfDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("TU"))
                .andExpect(jsonPath("$.countryName").value("Test United States"))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.asOfDate").value("2025-06-01"))
                .andExpect(jsonPath("$.totalEligibleEmployees").value(3))
                .andExpect(jsonPath("$.jobTitles", hasSize(2)))
                .andExpect(jsonPath("$.jobTitles[0].jobTitle").value("Junior Engineer"))
                .andExpect(jsonPath("$.jobTitles[0].employeeCount").value(1))
                .andExpect(jsonPath("$.jobTitles[0].averageSalary").value(50000.00))
                .andExpect(jsonPath("$.jobTitles[1].jobTitle").value("Senior Engineer"))
                .andExpect(jsonPath("$.jobTitles[1].employeeCount").value(2))
                .andExpect(jsonPath("$.jobTitles[1].averageSalary").value(100000.00));
    }

    @Test
    @DisplayName("should filter by specific job title when jobTitle param is provided")
    void shouldFilterBySpecificJobTitle() throws Exception {
        Employee e1 = createEmployee("REP_FILT_01", "filt1@example.com", countryUS, "Senior Engineer", LocalDate.of(2025, 1, 1), null, null);
        Employee e2 = createEmployee("REP_FILT_02", "filt2@example.com", countryUS, "Junior Engineer", LocalDate.of(2025, 1, 1), null, null);

        createCompensation(e1, new BigDecimal("100000.00"), LocalDate.of(2025, 1, 1), null);
        createCompensation(e2, new BigDecimal("50000.00"), LocalDate.of(2025, 1, 1), null);

        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-by-job-title")
                        .param("asOfDate", "2025-06-01")
                        .param("jobTitle", "Junior Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEligibleEmployees").value(1))
                .andExpect(jsonPath("$.jobTitles", hasSize(1)))
                .andExpect(jsonPath("$.jobTitles[0].jobTitle").value("Junior Engineer"))
                .andExpect(jsonPath("$.jobTitles[0].employeeCount").value(1))
                .andExpect(jsonPath("$.jobTitles[0].averageSalary").value(50000.00));
    }

    @Test
    @DisplayName("should return empty job titles and 0 total when country has no eligible employees")
    void shouldReturnEmptyJobTitlesWhenNoEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-by-job-title")
                        .param("asOfDate", "2025-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("TU"))
                .andExpect(jsonPath("$.totalEligibleEmployees").value(0))
                .andExpect(jsonPath("$.jobTitles", hasSize(0)));
    }

    @Test
    @DisplayName("should default to current date when asOfDate is not provided")
    void shouldDefaultToCurrentDateWhenAsOfDateNotSupplied() throws Exception {
        Employee e1 = createEmployee("REP_DEF_01", "repdef@example.com", countryUS, "Dev", LocalDate.of(2020, 1, 1), null, null);
        createCompensation(e1, new BigDecimal("75000.00"), LocalDate.of(2020, 1, 1), null);

        mockMvc.perform(get("/api/v1/reports/countries/TU/salary-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOfDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.eligibleEmployeeCount").value(1))
                .andExpect(jsonPath("$.averageSalary").value(75000.00));
    }
}
