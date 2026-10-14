package com.salarymanagement.salarymanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class UpdateEmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 10, message = "Country code must not exceed 10 characters")
    private String countryCode;

    @NotBlank(message = "Job title is required")
    @Size(max = 100, message = "Job title must not exceed 100 characters")
    private String jobTitle;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    private LocalDate terminatedAt;

    public UpdateEmployeeRequest() {
    }

    public UpdateEmployeeRequest(String firstName, String lastName, String email, String countryCode,
                                 String jobTitle, String department, LocalDate terminatedAt) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.countryCode = countryCode;
        this.jobTitle = jobTitle;
        this.department = department;
        this.terminatedAt = terminatedAt;
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

    public LocalDate getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(LocalDate terminatedAt) {
        this.terminatedAt = terminatedAt;
    }
}
