package com.salarymanagement.salarymanagement.seed;

public record SeedResult(
        boolean executed,
        boolean skipped,
        int employeesInserted,
        int compensationsInserted,
        long durationMs,
        String message
) {}
