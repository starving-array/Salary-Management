package com.salarymanagement.salarymanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salarymanagement.salarymanagement.dto.request.ChangeSalaryRequest;
import com.salarymanagement.salarymanagement.dto.request.CreateCompensationRequest;
import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.repository.CompensationRecordRepository;
import com.salarymanagement.salarymanagement.repository.CountryRepository;
import com.salarymanagement.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompensationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CompensationRecordRepository compensationRecordRepository;

    private Country countryUS;
    private Employee activeEmployee;

    @BeforeEach
    void setUp() {
        countryUS = countryRepository.findById("US")
                .orElseGet(() -> countryRepository.save(new Country("US", "United States", "USD")));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        activeEmployee = new Employee(
                UUID.randomUUID(),
                "EMP_COMP_" + suffix,
                "Charles",
                "Babbage",
                "charles." + suffix + "@example.com",
                countryUS,
                "Inventor",
                "Engineering",
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
        employeeRepository.save(activeEmployee);
    }

    @AfterEach
    void tearDown() {
        if (activeEmployee != null && activeEmployee.getId() != null) {
            try {
                compensationRecordRepository.deleteAll(compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(activeEmployee.getId()));
                if (employeeRepository.existsById(activeEmployee.getId())) {
                    employeeRepository.deleteById(activeEmployee.getId());
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("should assign initial salary and return 201 Created with Location header and USD currency")
    void shouldAssignInitialSalarySuccessfully() throws Exception {
        CreateCompensationRequest request = new CreateCompensationRequest(
                new BigDecimal("75000.00"),
                LocalDate.of(2025, 1, 1),
                null
        );

        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/compensations/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.employeeId").value(activeEmployee.getId().toString()))
                .andExpect(jsonPath("$.annualSalary").value(75000.00))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.effectiveFrom").value("2025-01-01"))
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        List<CompensationRecord> records = compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(activeEmployee.getId());
        assertEquals(1, records.size());
        assertEquals(new BigDecimal("75000.00"), records.get(0).getAnnualSalary());
        assertNull(records.get(0).getEffectiveTo());
    }

    @Test
    @DisplayName("should retrieve compensation history in deterministic ascending order")
    void shouldRetrieveOrderedCompensationHistory() throws Exception {
        // Initial record [2024-01-01, 2025-01-01)
        CompensationRecord r1 = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("60000.00"),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 1, 1),
                Instant.now()
        );
        // Second record [2025-01-01, 2026-01-01)
        CompensationRecord r2 = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("70000.00"),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 1),
                Instant.now()
        );
        // Third record [2026-01-01, null)
        CompensationRecord r3 = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("80000.00"),
                LocalDate.of(2026, 1, 1),
                null,
                Instant.now()
        );
        compensationRecordRepository.saveAll(List.of(r2, r1, r3)); // inserted in non-ordered sequence

        mockMvc.perform(get("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].effectiveFrom").value("2024-01-01"))
                .andExpect(jsonPath("$[0].annualSalary").value(60000.00))
                .andExpect(jsonPath("$[0].effectiveTo").value("2025-01-01"))
                .andExpect(jsonPath("$[1].effectiveFrom").value("2025-01-01"))
                .andExpect(jsonPath("$[1].annualSalary").value(70000.00))
                .andExpect(jsonPath("$[1].effectiveTo").value("2026-01-01"))
                .andExpect(jsonPath("$[2].effectiveFrom").value("2026-01-01"))
                .andExpect(jsonPath("$[2].annualSalary").value(80000.00))
                .andExpect(jsonPath("$[2].effectiveTo").doesNotExist());
    }

    @Test
    @DisplayName("should return HTTP 200 with empty array when active employee has no compensation records")
    void shouldReturnEmptyArrayWhenEmployeeHasNoCompensationRecords() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("should record salary change, closing prior record and inserting new record")
    void shouldRecordSalaryChangeAndPreserveOldRecord() throws Exception {
        CompensationRecord initial = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("70000.00"),
                LocalDate.of(2025, 1, 1),
                null,
                Instant.now()
        );
        compensationRecordRepository.save(initial);

        ChangeSalaryRequest changeRequest = new ChangeSalaryRequest(
                new BigDecimal("85000.00"),
                LocalDate.of(2025, 7, 1)
        );

        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations/changes", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.annualSalary").value(85000.00))
                .andExpect(jsonPath("$.effectiveFrom").value("2025-07-01"))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        List<CompensationRecord> records = compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(activeEmployee.getId());
        assertEquals(2, records.size());

        CompensationRecord oldRecord = records.get(0);
        assertEquals(new BigDecimal("70000.00"), oldRecord.getAnnualSalary());
        assertEquals(LocalDate.of(2025, 1, 1), oldRecord.getEffectiveFrom());
        assertEquals(LocalDate.of(2025, 7, 1), oldRecord.getEffectiveTo()); // Closed!

        CompensationRecord newRecord = records.get(1);
        assertEquals(new BigDecimal("85000.00"), newRecord.getAnnualSalary());
        assertEquals(LocalDate.of(2025, 7, 1), newRecord.getEffectiveFrom());
        assertNull(newRecord.getEffectiveTo());
    }

    @Test
    @DisplayName("should reject salary change on same date as prior record start boundary")
    void shouldRejectSalaryChangeOnSameDateAsPriorRecord() throws Exception {
        CompensationRecord initial = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("70000.00"),
                LocalDate.of(2025, 1, 1),
                null,
                Instant.now()
        );
        compensationRecordRepository.save(initial);

        ChangeSalaryRequest sameDateChange = new ChangeSalaryRequest(
                new BigDecimal("85000.00"),
                LocalDate.of(2025, 1, 1) // Same date as initial.effectiveFrom
        );

        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations/changes", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sameDateChange)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("No existing compensation record is effective immediately before")));
    }

    @Test
    @DisplayName("should reject zero, negative, and invalid salary decimal precision")
    void shouldRejectZeroNegativeAndInvalidSalaryValues() throws Exception {
        // Zero salary
        CreateCompensationRequest zeroSalary = new CreateCompensationRequest(
                BigDecimal.ZERO,
                LocalDate.of(2025, 1, 1),
                null
        );
        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroSalary)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Negative salary
        CreateCompensationRequest negativeSalary = new CreateCompensationRequest(
                new BigDecimal("-5000.00"),
                LocalDate.of(2025, 1, 1),
                null
        );
        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeSalary)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Excess decimal places (> 2)
        CreateCompensationRequest excessScale = new CreateCompensationRequest(
                new BigDecimal("50000.555"),
                LocalDate.of(2025, 1, 1),
                null
        );
        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessScale)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("should reject overlapping periods when attempting to create a second open-ended record")
    void shouldRejectOverlappingPeriods() throws Exception {
        CompensationRecord r1 = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("60000.00"),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 7, 1),
                Instant.now()
        );
        CompensationRecord r2 = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("70000.00"),
                LocalDate.of(2025, 7, 1),
                null,
                Instant.now()
        );
        compensationRecordRepository.saveAll(List.of(r1, r2));

        // Attempting an open-ended salary change within r1's duration without specifying effectiveTo
        ChangeSalaryRequest overlappingChange = new ChangeSalaryRequest(
                new BigDecimal("65000.00"),
                LocalDate.of(2025, 4, 1),
                null // null effectiveTo would overlap with r2 [2025-07-01, null)
        );

        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations/changes", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingChange)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("overlap with subsequent compensation record")));
    }

    @Test
    @DisplayName("should return 404 Not Found for missing and soft-deleted employees")
    void shouldReturnNotFoundForMissingAndSoftDeletedEmployees() throws Exception {
        UUID missingId = UUID.randomUUID();
        CreateCompensationRequest request = new CreateCompensationRequest(
                new BigDecimal("60000.00"),
                LocalDate.of(2025, 1, 1),
                null
        );

        // Missing employee
        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Soft-deleted employee
        activeEmployee.setDeletedAt(Instant.now());
        employeeRepository.save(activeEmployee);

        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("should return 404 Not Found on GET compensation history for missing and soft-deleted employees")
    void shouldReturnNotFoundOnGetCompensationHistoryForMissingAndSoftDeletedEmployees() throws Exception {
        UUID missingId = UUID.randomUUID();

        // Missing employee
        mockMvc.perform(get("/api/v1/employees/{employeeId}/compensations", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Soft-deleted employee
        activeEmployee.setDeletedAt(Instant.now());
        employeeRepository.save(activeEmployee);

        mockMvc.perform(get("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("should reject salary change without an applicable prior record")
    void shouldRejectSalaryChangeWithoutApplicablePriorRecord() throws Exception {
        ChangeSalaryRequest request = new ChangeSalaryRequest(
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 6, 1)
        );

        // Employee has no existing compensation records
        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations/changes", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("No existing compensation record found")));
    }

    @Test
    @DisplayName("should enforce employee join date and termination date boundaries")
    void shouldEnforceEmployeeTerminationBoundaries() throws Exception {
        activeEmployee.setJoinedAt(LocalDate.of(2025, 2, 1));
        activeEmployee.setTerminatedAt(LocalDate.of(2025, 12, 31));
        employeeRepository.save(activeEmployee);

        // 1. Effective date before join date
        CreateCompensationRequest beforeJoin = new CreateCompensationRequest(
                new BigDecimal("60000.00"),
                LocalDate.of(2025, 1, 1), // Before joinedAt 2025-02-01
                null
        );
        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beforeJoin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be before employee join date")));

        // 2. Effective date on/after termination date
        CreateCompensationRequest afterTermination = new CreateCompensationRequest(
                new BigDecimal("60000.00"),
                LocalDate.of(2025, 12, 31), // On terminatedAt
                null
        );
        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(afterTermination)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be on or after employee termination date")));
    }

    @Test
    @DisplayName("should rollback transaction if salary change fails")
    void shouldRollbackTransactionIfSalaryChangeFails() throws Exception {
        CompensationRecord initial = new CompensationRecord(
                UUID.randomUUID(),
                activeEmployee,
                new BigDecimal("70000.00"),
                LocalDate.of(2025, 1, 1),
                null,
                Instant.now()
        );
        compensationRecordRepository.save(initial);

        // Invalid salary change (salary = -100)
        ChangeSalaryRequest invalidChange = new ChangeSalaryRequest(
                new BigDecimal("-100.00"),
                LocalDate.of(2025, 7, 1)
        );

        mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations/changes", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidChange)))
                .andExpect(status().isBadRequest());

        // Verify initial record remains open-ended and untouched
        CompensationRecord reloaded = compensationRecordRepository.findById(initial.getId()).orElseThrow();
        assertNull(reloaded.getEffectiveTo(), "Initial record must not be closed if request failed validation");
        assertEquals(1, compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(activeEmployee.getId()).size());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("should serialize concurrent salary changes using pessimistic employee lock")
    void shouldHandleConcurrentSalaryChangesDeterministically() throws Exception {
        UUID empId = UUID.randomUUID();
        Employee emp = new Employee(
                empId,
                "EMP_CONCURRENT_01",
                "Concurrent",
                "Tester",
                "concurrent.tester@example.com",
                countryUS,
                "Concurrency Engineer",
                "QA",
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
        employeeRepository.save(emp);

        CompensationRecord initial = new CompensationRecord(
                UUID.randomUUID(),
                emp,
                new BigDecimal("60000.00"),
                LocalDate.of(2025, 1, 1),
                null,
                Instant.now()
        );
        compensationRecordRepository.save(initial);

        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Callable<Integer> task1 = () -> {
                    ChangeSalaryRequest req1 = new ChangeSalaryRequest(
                            new BigDecimal("70000.00"),
                            LocalDate.of(2025, 6, 1)
                    );
                    return mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations/changes", empId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req1)))
                            .andReturn().getResponse().getStatus();
                };

                Callable<Integer> task2 = () -> {
                    ChangeSalaryRequest req2 = new ChangeSalaryRequest(
                            new BigDecimal("80000.00"),
                            LocalDate.of(2025, 9, 1)
                    );
                    return mockMvc.perform(post("/api/v1/employees/{employeeId}/compensations/changes", empId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req2)))
                            .andReturn().getResponse().getStatus();
                };

                List<Future<Integer>> futures = executor.invokeAll(List.of(task1, task2));
                for (Future<Integer> f : futures) {
                    f.get();
                }

                List<CompensationRecord> records = compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(empId);
                assertTrue(records.size() >= 2);

                // Verify non-overlapping invariant
                for (int i = 0; i < records.size() - 1; i++) {
                    CompensationRecord current = records.get(i);
                    CompensationRecord next = records.get(i + 1);
                    assertNotNull(current.getEffectiveTo());
                    assertTrue(!current.getEffectiveTo().isAfter(next.getEffectiveFrom()),
                            "Current record effectiveTo must not be after next record effectiveFrom");
                }
            } finally {
                executor.shutdown();
            }
        } finally {
            compensationRecordRepository.deleteAll(compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(empId));
            employeeRepository.deleteById(empId);
        }
    }
}
