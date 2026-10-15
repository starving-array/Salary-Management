package com.salarymanagement.salarymanagement.controller;

import com.salarymanagement.salarymanagement.dto.response.CountryJobTitleSalaryResponse;
import com.salarymanagement.salarymanagement.dto.response.CountrySalarySummaryResponse;
import com.salarymanagement.salarymanagement.service.ReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/countries/{countryCode}/salary-summary")
    public ResponseEntity<CountrySalarySummaryResponse> getCountrySalarySummary(
            @PathVariable String countryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        CountrySalarySummaryResponse response = reportingService.getCountrySalarySummary(countryCode, asOfDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/countries/{countryCode}/salary-by-job-title")
    public ResponseEntity<CountryJobTitleSalaryResponse> getSalaryByJobTitle(
            @PathVariable String countryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) String jobTitle) {
        CountryJobTitleSalaryResponse response = reportingService.getSalaryByJobTitle(countryCode, asOfDate, jobTitle);
        return ResponseEntity.ok(response);
    }
}
