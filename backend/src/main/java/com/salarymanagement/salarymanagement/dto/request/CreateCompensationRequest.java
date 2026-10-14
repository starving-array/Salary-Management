package com.salarymanagement.salarymanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateCompensationRequest {

    @NotNull(message = "Annual salary is required")
    @DecimalMin(value = "0.01", message = "Annual salary must be positive")
    @Digits(integer = 13, fraction = 2, message = "Annual salary cannot exceed 2 decimal places")
    private BigDecimal annualSalary;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    public CreateCompensationRequest() {
    }

    public CreateCompensationRequest(BigDecimal annualSalary, LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.annualSalary = annualSalary;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public BigDecimal getAnnualSalary() {
        return annualSalary;
    }

    public void setAnnualSalary(BigDecimal annualSalary) {
        this.annualSalary = annualSalary;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}
