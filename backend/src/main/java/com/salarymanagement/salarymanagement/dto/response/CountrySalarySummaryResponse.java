package com.salarymanagement.salarymanagement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CountrySalarySummaryResponse {

    private String countryCode;
    private String countryName;
    private String currencyCode;
    private LocalDate asOfDate;
    private long eligibleEmployeeCount;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private BigDecimal averageSalary;

    public CountrySalarySummaryResponse() {
    }

    public CountrySalarySummaryResponse(String countryCode, String countryName, String currencyCode,
                                        LocalDate asOfDate, long eligibleEmployeeCount,
                                        BigDecimal minSalary, BigDecimal maxSalary, BigDecimal averageSalary) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.currencyCode = currencyCode;
        this.asOfDate = asOfDate;
        this.eligibleEmployeeCount = eligibleEmployeeCount;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.averageSalary = averageSalary;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public long getEligibleEmployeeCount() {
        return eligibleEmployeeCount;
    }

    public void setEligibleEmployeeCount(long eligibleEmployeeCount) {
        this.eligibleEmployeeCount = eligibleEmployeeCount;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
    }

    public BigDecimal getAverageSalary() {
        return averageSalary;
    }

    public void setAverageSalary(BigDecimal averageSalary) {
        this.averageSalary = averageSalary;
    }
}
