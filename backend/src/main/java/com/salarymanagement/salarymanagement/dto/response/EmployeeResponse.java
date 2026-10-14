package com.salarymanagement.salarymanagement.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public class EmployeeResponse {

    private UUID id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String countryCode;
    private String jobTitle;
    private String department;
    private LocalDate joinedAt;
    private LocalDate terminatedAt;

    public EmployeeResponse() {
    }

    public EmployeeResponse(UUID id, String employeeNumber, String firstName, String lastName,
                            String email, String countryCode, String jobTitle, String department,
                            LocalDate joinedAt, LocalDate terminatedAt) {
        this.id = id;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.countryCode = countryCode;
        this.jobTitle = jobTitle;
        this.department = department;
        this.joinedAt = joinedAt;
        this.terminatedAt = terminatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDate joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDate getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(LocalDate terminatedAt) {
        this.terminatedAt = terminatedAt;
    }
}
