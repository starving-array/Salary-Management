package com.salarymanagement.salarymanagement.repository;

import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class SalaryManagementRepositoryTests {

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CompensationRecordRepository compensationRecordRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("should persist and retrieve country, employee, and compensation record entities")
    void shouldPersistAndRetrieveEntities() {
        Country country = new Country("US", "United States", "USD");
        countryRepository.save(country);

        UUID employeeId = UUID.randomUUID();
        LocalDate joinedAt = LocalDate.of(2025, 1, 15);
        Employee employee = new Employee(
                employeeId,
                "EMP001",
                "Alice",
                "Smith",
                "alice.smith@example.com",
                country,
                "Software Engineer",
                "Engineering",
                joinedAt,
                null,
                null
        );
        employeeRepository.save(employee);

        UUID compensationId = UUID.randomUUID();
        BigDecimal salary = new BigDecimal("120000.00");
        LocalDate effectiveFrom = LocalDate.of(2025, 1, 15);
        Instant createdAt = Instant.now();
        CompensationRecord compensation = new CompensationRecord(
                compensationId,
                employee,
                salary,
                effectiveFrom,
                null,
                createdAt
        );
        compensationRecordRepository.save(compensation);

        // Flush to database and clear first-level cache to verify actual SQL persistence
        entityManager.flush();
        entityManager.clear();

        Optional<Country> foundCountry = countryRepository.findById("US");
        assertTrue(foundCountry.isPresent());
        assertEquals("United States", foundCountry.get().getName());
        assertEquals("USD", foundCountry.get().getCurrencyCode());

        Optional<Employee> foundEmployee = employeeRepository.findById(employeeId);
        assertTrue(foundEmployee.isPresent());
        assertEquals("EMP001", foundEmployee.get().getEmployeeNumber());
        assertEquals("Alice", foundEmployee.get().getFirstName());
        assertEquals("US", foundEmployee.get().getCountry().getCode());

        Optional<CompensationRecord> foundRecord = compensationRecordRepository.findById(compensationId);
        assertTrue(foundRecord.isPresent());
        assertEquals(0, salary.compareTo(foundRecord.get().getAnnualSalary()));
        assertEquals(effectiveFrom, foundRecord.get().getEffectiveFrom());
        assertNotNull(foundRecord.get().getCreatedAt());
        assertEquals(employeeId, foundRecord.get().getEmployee().getId());
    }
}
