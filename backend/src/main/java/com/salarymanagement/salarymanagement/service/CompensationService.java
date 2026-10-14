package com.salarymanagement.salarymanagement.service;

import com.salarymanagement.salarymanagement.dto.request.ChangeSalaryRequest;
import com.salarymanagement.salarymanagement.dto.request.CreateCompensationRequest;
import com.salarymanagement.salarymanagement.dto.response.CompensationResponse;
import com.salarymanagement.salarymanagement.entity.CompensationRecord;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.exception.DuplicateResourceException;
import com.salarymanagement.salarymanagement.exception.ResourceNotFoundException;
import com.salarymanagement.salarymanagement.mapper.CompensationMapper;
import com.salarymanagement.salarymanagement.repository.CompensationRecordRepository;
import com.salarymanagement.salarymanagement.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CompensationService {

    private final CompensationRecordRepository compensationRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final CompensationMapper compensationMapper;

    public CompensationService(CompensationRecordRepository compensationRecordRepository,
                               EmployeeRepository employeeRepository,
                               CompensationMapper compensationMapper) {
        this.compensationRecordRepository = compensationRecordRepository;
        this.employeeRepository = employeeRepository;
        this.compensationMapper = compensationMapper;
    }

    @Transactional
    public CompensationResponse assignInitialSalary(UUID employeeId, CreateCompensationRequest request) {
        if (employeeId == null) {
            throw new ResourceNotFoundException("Employee id must not be null");
        }

        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (compensationRecordRepository.existsByEmployee_Id(employeeId)) {
            throw new DuplicateResourceException("Employee already has compensation records. Use the salary change endpoint to record compensation changes.");
        }

        validateEmploymentDates(employee, request.getEffectiveFrom(), request.getEffectiveTo());

        CompensationRecord record = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                request.getAnnualSalary(),
                request.getEffectiveFrom(),
                request.getEffectiveTo(),
                Instant.now()
        );

        CompensationRecord saved = compensationRecordRepository.save(record);
        return compensationMapper.toResponse(saved, employee.getCountry().getCurrencyCode());
    }

    @Transactional(readOnly = true)
    public List<CompensationResponse> getCompensationHistory(UUID employeeId) {
        if (employeeId == null) {
            throw new ResourceNotFoundException("Employee id must not be null");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        List<CompensationRecord> records = compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(employeeId);
        return compensationMapper.toResponseList(records, employee.getCountry().getCurrencyCode());
    }

    @Transactional
    public CompensationResponse recordSalaryChange(UUID employeeId, ChangeSalaryRequest request) {
        if (employeeId == null) {
            throw new ResourceNotFoundException("Employee id must not be null");
        }

        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        validateEmploymentDates(employee, request.getEffectiveFrom(), request.getEffectiveTo());

        List<CompensationRecord> existingRecords = compensationRecordRepository.findByEmployee_IdOrderByEffectiveFromAscCreatedAtAsc(employeeId);
        if (existingRecords.isEmpty()) {
            throw new IllegalArgumentException("No existing compensation record found. Assign an initial salary before recording changes.");
        }

        CompensationRecord priorRecord = null;
        for (CompensationRecord r : existingRecords) {
            if (r.getEffectiveFrom().isBefore(request.getEffectiveFrom())
                    && (r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(request.getEffectiveFrom()))) {
                priorRecord = r;
                break;
            }
        }

        if (priorRecord == null) {
            throw new IllegalArgumentException("No existing compensation record is effective immediately before the requested change date: " + request.getEffectiveFrom());
        }

        if (priorRecord.getEffectiveTo() != null) {
            if (request.getEffectiveTo() == null) {
                throw new IllegalArgumentException("Requested open-ended salary change would overlap with subsequent compensation record starting at: " + priorRecord.getEffectiveTo());
            }
            if (request.getEffectiveTo().isAfter(priorRecord.getEffectiveTo())) {
                throw new IllegalArgumentException("Requested compensation period overlaps with subsequent compensation record starting at: " + priorRecord.getEffectiveTo());
            }
        }

        // Close prior record at the effective start date of the new record
        priorRecord.setEffectiveTo(request.getEffectiveFrom());

        // Validate that the new record does not overlap with any existing record
        for (CompensationRecord r : existingRecords) {
            if (overlaps(request.getEffectiveFrom(), request.getEffectiveTo(), r.getEffectiveFrom(), r.getEffectiveTo())) {
                throw new IllegalArgumentException("Requested compensation period overlaps with an existing record: [" + r.getEffectiveFrom() + ", " + r.getEffectiveTo() + ")");
            }
        }

        // Flush updated prior record to PostgreSQL so uq_compensation_open_ended is cleared
        compensationRecordRepository.saveAndFlush(priorRecord);

        CompensationRecord newRecord = new CompensationRecord(
                UUID.randomUUID(),
                employee,
                request.getAnnualSalary(),
                request.getEffectiveFrom(),
                request.getEffectiveTo(),
                Instant.now()
        );

        CompensationRecord saved = compensationRecordRepository.save(newRecord);
        return compensationMapper.toResponse(saved, employee.getCountry().getCurrencyCode());
    }

    private void validateEmploymentDates(Employee employee, LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveFrom.isBefore(employee.getJoinedAt())) {
            throw new IllegalArgumentException("Compensation effective date cannot be before employee join date: " + employee.getJoinedAt());
        }

        if (employee.getTerminatedAt() != null && !effectiveFrom.isBefore(employee.getTerminatedAt())) {
            throw new IllegalArgumentException("Compensation effective date cannot be on or after employee termination date: " + employee.getTerminatedAt());
        }

        if (effectiveTo != null) {
            if (!effectiveTo.isAfter(effectiveFrom)) {
                throw new IllegalArgumentException("Effective to date must be after effective from date");
            }
            if (employee.getTerminatedAt() != null && effectiveTo.isAfter(employee.getTerminatedAt())) {
                throw new IllegalArgumentException("Compensation effective to date cannot be after employee termination date: " + employee.getTerminatedAt());
            }
        }
    }

    private boolean overlaps(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
        LocalDate maxStart = aFrom.isAfter(bFrom) ? aFrom : bFrom;
        if (aTo != null && bTo != null) {
            LocalDate minEnd = aTo.isBefore(bTo) ? aTo : bTo;
            return maxStart.isBefore(minEnd);
        } else if (aTo != null) {
            return maxStart.isBefore(aTo);
        } else if (bTo != null) {
            return maxStart.isBefore(bTo);
        } else {
            return true;
        }
    }
}
