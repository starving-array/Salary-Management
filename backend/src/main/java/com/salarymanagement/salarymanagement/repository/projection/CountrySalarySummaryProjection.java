package com.salarymanagement.salarymanagement.repository.projection;

import java.math.BigDecimal;

public interface CountrySalarySummaryProjection {

    Long getEmployeeCount();

    BigDecimal getMinSalary();

    BigDecimal getMaxSalary();

    Double getAverageSalary();
}
