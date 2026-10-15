package com.salarymanagement.salarymanagement.dto.response;

import java.time.LocalDate;
import java.util.List;

public class CountryJobTitleSalaryResponse {

    private String countryCode;
    private String countryName;
    private String currencyCode;
    private LocalDate asOfDate;
    private long totalEligibleEmployees;
    private List<JobTitleSalaryResponse> jobTitles;

    public CountryJobTitleSalaryResponse() {
    }

    public CountryJobTitleSalaryResponse(String countryCode, String countryName, String currencyCode,
                                         LocalDate asOfDate, long totalEligibleEmployees,
                                         List<JobTitleSalaryResponse> jobTitles) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.currencyCode = currencyCode;
        this.asOfDate = asOfDate;
        this.totalEligibleEmployees = totalEligibleEmployees;
        this.jobTitles = jobTitles;
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

    public long getTotalEligibleEmployees() {
        return totalEligibleEmployees;
    }

    public void setTotalEligibleEmployees(long totalEligibleEmployees) {
        this.totalEligibleEmployees = totalEligibleEmployees;
    }

    public List<JobTitleSalaryResponse> getJobTitles() {
        return jobTitles;
    }

    public void setJobTitles(List<JobTitleSalaryResponse> jobTitles) {
        this.jobTitles = jobTitles;
    }
}
