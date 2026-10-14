package com.salarymanagement.salarymanagement.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class CompensationResponse {

    private UUID id;
    private UUID employeeId;
    private BigDecimal annualSalary;
    private String currencyCode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Instant createdAt;

    public CompensationResponse() {
    }

    public CompensationResponse(UUID id, UUID employeeId, BigDecimal annualSalary,
                                String currencyCode, LocalDate effectiveFrom,
                                LocalDate effectiveTo, Instant createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.annualSalary = annualSalary;
        this.currencyCode = currencyCode;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public BigDecimal getAnnualSalary() {
        return annualSalary;
    }

    public void setAnnualSalary(BigDecimal annualSalary) {
        this.annualSalary = annualSalary;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
