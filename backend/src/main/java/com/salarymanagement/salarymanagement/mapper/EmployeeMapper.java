package com.salarymanagement.salarymanagement.mapper;

import com.salarymanagement.salarymanagement.dto.request.CreateEmployeeRequest;
import com.salarymanagement.salarymanagement.dto.response.EmployeeResponse;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.entity.Employee;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EmployeeMapper {

    public Employee toEntity(CreateEmployeeRequest request, Country country) {
        if (request == null) {
            return null;
        }
        return new Employee(
                UUID.randomUUID(),
                request.getEmployeeNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                country,
                request.getJobTitle(),
                request.getDepartment(),
                request.getJoinedAt(),
                request.getTerminatedAt(),
                null
        );
    }

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) {
            return null;
        }
        String countryCode = employee.getCountry() != null ? employee.getCountry().getCode() : null;
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                countryCode,
                employee.getJobTitle(),
                employee.getDepartment(),
                employee.getJoinedAt(),
                employee.getTerminatedAt()
        );
    }
}
