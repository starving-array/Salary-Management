package com.salarymanagement.salarymanagement.service;

import com.salarymanagement.salarymanagement.dto.response.CountryJobTitleSalaryResponse;
import com.salarymanagement.salarymanagement.dto.response.CountrySalarySummaryResponse;
import com.salarymanagement.salarymanagement.dto.response.JobTitleSalaryResponse;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.exception.ResourceNotFoundException;
import com.salarymanagement.salarymanagement.repository.CompensationRecordRepository;
import com.salarymanagement.salarymanagement.repository.CountryRepository;
import com.salarymanagement.salarymanagement.repository.projection.CountrySalarySummaryProjection;
import com.salarymanagement.salarymanagement.repository.projection.JobTitleSalarySummaryProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportingService {

    private final CompensationRecordRepository compensationRecordRepository;
    private final CountryRepository countryRepository;

    public ReportingService(CompensationRecordRepository compensationRecordRepository,
                            CountryRepository countryRepository) {
        this.compensationRecordRepository = compensationRecordRepository;
        this.countryRepository = countryRepository;
    }

    @Transactional(readOnly = true)
    public CountrySalarySummaryResponse getCountrySalarySummary(String countryCode, LocalDate asOfDate) {
        return getCountrySalarySummary(countryCode, asOfDate, null);
    }

    @Transactional(readOnly = true)
    public CountrySalarySummaryResponse getCountrySalarySummary(String countryCode, LocalDate asOfDate, String jobTitle) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new ResourceNotFoundException("Country not found with code: " + countryCode);
        }

        Country country = countryRepository.findById(countryCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with code: " + countryCode));

        LocalDate effectiveDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        CountrySalarySummaryProjection projection;
        if (jobTitle != null && !jobTitle.isBlank()) {
            projection = compensationRecordRepository.getCountrySalarySummaryByJobTitle(country.getCode(), effectiveDate, jobTitle.trim());
        } else {
            projection = compensationRecordRepository.getCountrySalarySummary(country.getCode(), effectiveDate);
        }

        if (projection == null || projection.getEmployeeCount() == null || projection.getEmployeeCount() == 0L) {
            return new CountrySalarySummaryResponse(
                    country.getCode(),
                    country.getName(),
                    country.getCurrencyCode(),
                    effectiveDate,
                    0L,
                    null,
                    null,
                    null
            );
        }

        BigDecimal minSalary = projection.getMinSalary() != null
                ? projection.getMinSalary().setScale(2, RoundingMode.HALF_UP)
                : null;
        BigDecimal maxSalary = projection.getMaxSalary() != null
                ? projection.getMaxSalary().setScale(2, RoundingMode.HALF_UP)
                : null;
        BigDecimal avgSalary = projection.getAverageSalary() != null
                ? BigDecimal.valueOf(projection.getAverageSalary()).setScale(2, RoundingMode.HALF_UP)
                : null;

        return new CountrySalarySummaryResponse(
                country.getCode(),
                country.getName(),
                country.getCurrencyCode(),
                effectiveDate,
                projection.getEmployeeCount(),
                minSalary,
                maxSalary,
                avgSalary
        );
    }

    @Transactional(readOnly = true)
    public CountryJobTitleSalaryResponse getSalaryByJobTitle(String countryCode, LocalDate asOfDate, String jobTitle) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new ResourceNotFoundException("Country not found with code: " + countryCode);
        }

        Country country = countryRepository.findById(countryCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with code: " + countryCode));

        LocalDate effectiveDate = (asOfDate != null) ? asOfDate : LocalDate.now();

        List<JobTitleSalarySummaryProjection> projections;
        if (jobTitle != null && !jobTitle.isBlank()) {
            projections = compensationRecordRepository.getSalaryByJobTitleAndCountry(country.getCode(), effectiveDate, jobTitle.trim());
        } else {
            projections = compensationRecordRepository.getSalaryByJobTitle(country.getCode(), effectiveDate);
        }

        List<JobTitleSalaryResponse> jobTitleResponses = (projections != null) ? projections.stream()
                .map(p -> new JobTitleSalaryResponse(
                        p.getJobTitle(),
                        p.getEmployeeCount() != null ? p.getEmployeeCount() : 0L,
                        p.getAverageSalary() != null
                                ? BigDecimal.valueOf(p.getAverageSalary()).setScale(2, RoundingMode.HALF_UP)
                                : null
                ))
                .toList() : List.of();

        long totalEligibleEmployees = jobTitleResponses.stream()
                .mapToLong(JobTitleSalaryResponse::getEmployeeCount)
                .sum();

        return new CountryJobTitleSalaryResponse(
                country.getCode(),
                country.getName(),
                country.getCurrencyCode(),
                effectiveDate,
                totalEligibleEmployees,
                jobTitleResponses
        );
    }
}
