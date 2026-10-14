package com.salarymanagement.salarymanagement.service;

import com.salarymanagement.salarymanagement.dto.request.ChangeSalaryRequest;
import com.salarymanagement.salarymanagement.dto.request.CreateCompensationRequest;
import com.salarymanagement.salarymanagement.dto.response.CompensationResponse;
import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.exception.DuplicateResourceException;
import com.salarymanagement.salarymanagement.exception.ResourceNotFoundException;
import com.salarymanagement.salarymanagement.mapper.CompensationMapper;
import com.salarymanagement.salarymanagement.repository.CompensationRecordRepository;
import com.salarymanagement.salarymanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationServiceTests {

    @Mock
    private CompensationRecordRepository compensationRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Spy
    private CompensationMapper compensationMapper = new CompensationMapper();

    @InjectMocks
    private CompensationService compensationService;

    private Employee employee;
    private Country country;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        country = new Country("US", "United States", "USD");
        employeeId = UUID.randomUUID();
        employee = new Employee(
                employeeId,
                "EMP3001",
                "Ada",
                "Lovelace",
                "ada.lovelace@example.com",
                country,
                "Mathematician",
                "Computing",
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
    }

    @Test
    @DisplayName("should assign initial salary successfully")
    void shouldAssignInitialSalarySuccessfully() {
        CreateCompensationRequest request = new CreateCompensationRequest(
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                null
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.existsByEmployee_Id(employeeId)).thenReturn(false);
        when(compensationRecordRepository.save(any(CompensationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        CompensationResponse response = compensationService.assignInitialSalary(employeeId, request);

        assertNotNull(response);
        assertEquals(employeeId, response.getEmployeeId());
        assertEquals(new BigDecimal("80000.00"), response.getAnnualSalary());
        assertEquals("USD", response.getCurrencyCode());
        assertEquals(LocalDate.of(2025, 1, 1), response.getEffectiveFrom());
        assertNull(response.getEffectiveTo());
        verify(compensationRecordRepository).save(any(CompensationRecord.class));
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when employee already has compensation records")
    void shouldThrowExceptionWhenInitialSalaryAlreadyAssigned() {
        CreateCompensationRequest request = new CreateCompensationRequest(
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                null
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.existsByEmployee_Id(employeeId)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                compensationService.assignInitialSalary(employeeId, request)
        );

        assertTrue(ex.getMessage().contains("already has compensation records"));
        verify(compensationRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when effective date is before join date")
    void shouldThrowExceptionWhenEffectiveDateBeforeJoinDate() {
        CreateCompensationRequest request = new CreateCompensationRequest(
                new BigDecimal("80000.00"),
                LocalDate.of(2024, 12, 1), // Before joinedAt 2025-01-01
                null
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.existsByEmployee_Id(employeeId)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                compensationService.assignInitialSalary(employeeId, request)
        );

        assertTrue(ex.getMessage().contains("cannot be before employee join date"));
        verify(compensationRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when effective date is on or after termination date")
    void shouldThrowExceptionWhenEffectiveDateOnOrAfterTerminationDate() {
        employee.setTerminatedAt(LocalDate.of(2025, 6, 1));
        CreateCompensationRequest request = new CreateCompensationRequest(
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 6, 1), // Exactly on terminatedAt
                null
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.existsByEmployee_Id(employeeId)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                compensationService.assignInitialSalary(employeeId, request)
        );

        assertTrue(ex.getMessage().contains("cannot be on or after employee termination date"));
        verify(compensationRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("should record salary change and close prior record")
    void shouldRecordSalaryChangeSuccessfully() {
        CompensationRecord priorRecord = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                null,
                Instant.now()
        );
        List<CompensationRecord> records = new ArrayList<>(List.of(priorRecord));

        ChangeSalaryRequest request = new ChangeSalaryRequest(
                new BigDecimal("90000.00"),
                LocalDate.of(2025, 7, 1)
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(employeeId)).thenReturn(records);
        when(compensationRecordRepository.save(any(CompensationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        CompensationResponse response = compensationService.recordSalaryChange(employeeId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("90000.00"), response.getAnnualSalary());
        assertEquals(LocalDate.of(2025, 7, 1), response.getEffectiveFrom());
        assertNull(response.getEffectiveTo());

        // Verify prior record was closed at new record's effectiveFrom
        assertEquals(LocalDate.of(2025, 7, 1), priorRecord.getEffectiveTo());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when no prior record exists for salary change")
    void shouldThrowExceptionWhenNoPriorRecordForChange() {
        ChangeSalaryRequest request = new ChangeSalaryRequest(
                new BigDecimal("90000.00"),
                LocalDate.of(2025, 7, 1)
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(employeeId)).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                compensationService.recordSalaryChange(employeeId, request)
        );

        assertTrue(ex.getMessage().contains("No existing compensation record found"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when change effective date is not after prior record start")
    void shouldThrowExceptionWhenChangeDateSameAsPriorRecordStart() {
        CompensationRecord priorRecord = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                null,
                Instant.now()
        );

        ChangeSalaryRequest request = new ChangeSalaryRequest(
                new BigDecimal("90000.00"),
                LocalDate.of(2025, 1, 1) // Same date as priorRecord.effectiveFrom
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(employeeId)).thenReturn(List.of(priorRecord));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                compensationService.recordSalaryChange(employeeId, request)
        );

        assertTrue(ex.getMessage().contains("No existing compensation record is effective immediately before"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when open-ended change would overlap subsequent record")
    void shouldThrowExceptionWhenOpenEndedChangeOverlapsSubsequentRecord() {
        CompensationRecord r1 = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 1),
                Instant.now()
        );
        CompensationRecord r2 = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                new BigDecimal("100000.00"),
                LocalDate.of(2026, 1, 1),
                null,
                Instant.now()
        );

        // Attempt open-ended change within r1's period without specifying effectiveTo
        ChangeSalaryRequest request = new ChangeSalaryRequest(
                new BigDecimal("90000.00"),
                LocalDate.of(2025, 7, 1),
                null // null effectiveTo would overlap with r2
        );

        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(employeeId)).thenReturn(List.of(r1, r2));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                compensationService.recordSalaryChange(employeeId, request)
        );

        assertTrue(ex.getMessage().contains("overlap with subsequent compensation record"));
    }

    @Test
    @DisplayName("should retrieve compensation history in deterministic order")
    void shouldRetrieveCompensationHistory() {
        CompensationRecord r1 = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 7, 1),
                Instant.now()
        );
        CompensationRecord r2 = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                new BigDecimal("90000.00"),
                LocalDate.of(2025, 7, 1),
                null,
                Instant.now()
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(employeeId)).thenReturn(List.of(r1, r2));

        List<CompensationResponse> history = compensationService.getCompensationHistory(employeeId);

        assertEquals(2, history.size());
        assertEquals(new BigDecimal("80000.00"), history.get(0).getAnnualSalary());
        assertEquals(new BigDecimal("90000.00"), history.get(1).getAnnualSalary());
        assertEquals("USD", history.get(0).getCurrencyCode());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when employee is soft-deleted")
    void shouldThrowExceptionWhenEmployeeIsSoftDeleted() {
        employee.setDeletedAt(Instant.now());
        when(employeeRepository.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));

        CreateCompensationRequest request = new CreateCompensationRequest(
                new BigDecimal("80000.00"),
                LocalDate.of(2025, 1, 1),
                null
        );

        assertThrows(ResourceNotFoundException.class, () ->
                compensationService.assignInitialSalary(employeeId, request)
        );
    }
}
