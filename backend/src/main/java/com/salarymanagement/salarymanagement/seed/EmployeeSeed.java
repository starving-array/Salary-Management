package com.salarymanagement.salarymanagement.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeSeed(
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String countryCode,
        String jobTitle,
        String department,
        LocalDate joinedAt,
        LocalDate terminatedAt,
        Instant deletedAt
) {}
