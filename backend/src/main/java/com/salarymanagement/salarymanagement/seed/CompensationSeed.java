package com.salarymanagement.salarymanagement.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CompensationSeed(
        UUID id,
        UUID employeeId,
        BigDecimal annualSalary,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant createdAt
) {}
