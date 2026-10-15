package com.salarymanagement.salarymanagement.repository.projection;

public interface JobTitleSalarySummaryProjection {

    String getJobTitle();

    Long getEmployeeCount();

    Double getAverageSalary();
}
