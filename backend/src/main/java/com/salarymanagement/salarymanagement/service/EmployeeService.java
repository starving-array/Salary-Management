package com.salarymanagement.salarymanagement.service;

import com.salarymanagement.salarymanagement.dto.request.CreateEmployeeRequest;
import com.salarymanagement.salarymanagement.dto.request.UpdateEmployeeRequest;
import com.salarymanagement.salarymanagement.dto.response.EmployeeResponse;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import com.salarymanagement.salarymanagement.exception.CountryNotFoundException;
import com.salarymanagement.salarymanagement.exception.DuplicateResourceException;
import com.salarymanagement.salarymanagement.exception.ResourceNotFoundException;
import com.salarymanagement.salarymanagement.mapper.EmployeeMapper;
import com.salarymanagement.salarymanagement.repository.CountryRepository;
import com.salarymanagement.salarymanagement.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CountryRepository countryRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           CountryRepository countryRepository,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.countryRepository = countryRepository;
        this.employeeMapper = employeeMapper;
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (request.getTerminatedAt() != null && request.getTerminatedAt().isBefore(request.getJoinedAt())) {
            throw new IllegalArgumentException("Termination date cannot be before joining date");
        }

        Country country = countryRepository.findById(request.getCountryCode())
                .orElseThrow(() -> new CountryNotFoundException("Country not found with code: " + request.getCountryCode()));

        if (employeeRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new DuplicateResourceException("Employee number already exists: " + request.getEmployeeNumber());
        }

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        Employee employee = employeeMapper.toEntity(request, country);
        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Employee id must not be null");
        }
        Employee employee = employeeRepository.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, UpdateEmployeeRequest request) {
        if (id == null) {
            throw new ResourceNotFoundException("Employee id must not be null");
        }

        Employee employee = employeeRepository.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (request.getTerminatedAt() != null && request.getTerminatedAt().isBefore(employee.getJoinedAt())) {
            throw new IllegalArgumentException("Termination date cannot be before joining date");
        }

        if (request.getCountryCode() != null && !request.getCountryCode().isBlank()) {
            if (!countryRepository.existsById(request.getCountryCode())) {
                throw new CountryNotFoundException("Country not found with code: " + request.getCountryCode());
            }
            if (!request.getCountryCode().equalsIgnoreCase(employee.getCountry().getCode())) {
                throw new IllegalArgumentException("Changing employee country is not allowed: country-transfer workflows are excluded");
            }
        }

        if (!request.getEmail().equalsIgnoreCase(employee.getEmail())) {
            if (employeeRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
        }

        employeeMapper.updateEntity(employee, request);
        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(savedEmployee);
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Employee id must not be null");
        }

        Employee employee = employeeRepository.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employee.setDeletedAt(Instant.now());
        employeeRepository.save(employee);
    }
}
