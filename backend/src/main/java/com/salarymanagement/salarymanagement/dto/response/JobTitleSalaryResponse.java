package com.salarymanagement.salarymanagement.dto.response;

import java.math.BigDecimal;

public class JobTitleSalaryResponse {

    private String jobTitle;
    private long employeeCount;
    private BigDecimal averageSalary;

    public JobTitleSalaryResponse() {
    }

    public JobTitleSalaryResponse(String jobTitle, long employeeCount, BigDecimal averageSalary) {
        this.jobTitle = jobTitle;
        this.employeeCount = employeeCount;
        this.averageSalary = averageSalary;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public long getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(long employeeCount) {
        this.employeeCount = employeeCount;
    }

    public BigDecimal getAverageSalary() {
        return averageSalary;
    }

    public void setAverageSalary(BigDecimal averageSalary) {
        this.averageSalary = averageSalary;
    }
}
