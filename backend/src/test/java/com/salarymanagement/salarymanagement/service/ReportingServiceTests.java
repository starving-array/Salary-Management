package com.salarymanagement.salarymanagement.service;

import com.salarymanagement.salarymanagement.dto.response.CountryJobTitleSalaryResponse;
import com.salarymanagement.salarymanagement.dto.response.CountrySalarySummaryResponse;
import com.salarymanagement.salarymanagement.entity.Country;
import com.salarymanagement.salarymanagement.exception.ResourceNotFoundException;
import com.salarymanagement.salarymanagement.repository.CompensationRecordRepository;
import com.salarymanagement.salarymanagement.repository.CountryRepository;
import com.salarymanagement.salarymanagement.repository.projection.CountrySalarySummaryProjection;
import com.salarymanagement.salarymanagement.repository.projection.JobTitleSalarySummaryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTests {

    @Mock
    private CompensationRecordRepository compensationRecordRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private ReportingService reportingService;

    private Country countryUS;

    @BeforeEach
    void setUp() {
        countryUS = new Country("US", "United States", "USD");
    }

    @Test
    @DisplayName("should return salary summary when eligible employees exist")
    void shouldReturnSalarySummaryWhenEligibleEmployeesExist() {
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);
        CountrySalarySummaryProjection projection = mock(CountrySalarySummaryProjection.class);
        when(projection.getEmployeeCount()).thenReturn(3L);
        when(projection.getMinSalary()).thenReturn(new BigDecimal("50000.00"));
        when(projection.getMaxSalary()).thenReturn(new BigDecimal("90000.00"));
        when(projection.getAverageSalary()).thenReturn(70000.00);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getCountrySalarySummary("US", asOfDate)).thenReturn(projection);

        CountrySalarySummaryResponse response = reportingService.getCountrySalarySummary("US", asOfDate);

        assertNotNull(response);
        assertEquals("US", response.getCountryCode());
        assertEquals("United States", response.getCountryName());
        assertEquals("USD", response.getCurrencyCode());
        assertEquals(asOfDate, response.getAsOfDate());
        assertEquals(3L, response.getEligibleEmployeeCount());
        assertEquals(new BigDecimal("50000.00"), response.getMinSalary());
        assertEquals(new BigDecimal("90000.00"), response.getMaxSalary());
        assertEquals(new BigDecimal("70000.00"), response.getAverageSalary());
    }

    @Test
    @DisplayName("should return clear no-data summary with null statistics when count is zero")
    void shouldReturnNoDataSummaryWhenNoEligibleEmployeesExist() {
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);
        CountrySalarySummaryProjection projection = mock(CountrySalarySummaryProjection.class);
        when(projection.getEmployeeCount()).thenReturn(0L);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getCountrySalarySummary("US", asOfDate)).thenReturn(projection);

        CountrySalarySummaryResponse response = reportingService.getCountrySalarySummary("US", asOfDate);

        assertNotNull(response);
        assertEquals("US", response.getCountryCode());
        assertEquals(0L, response.getEligibleEmployeeCount());
        assertNull(response.getMinSalary());
        assertNull(response.getMaxSalary());
        assertNull(response.getAverageSalary());
    }

    @Test
    @DisplayName("should default to current date when asOfDate is null")
    void shouldDefaultToCurrentDateWhenAsOfDateIsNull() {
        CountrySalarySummaryProjection projection = mock(CountrySalarySummaryProjection.class);
        when(projection.getEmployeeCount()).thenReturn(1L);
        when(projection.getMinSalary()).thenReturn(new BigDecimal("60000.00"));
        when(projection.getMaxSalary()).thenReturn(new BigDecimal("60000.00"));
        when(projection.getAverageSalary()).thenReturn(60000.00);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getCountrySalarySummary(eq("US"), any(LocalDate.class))).thenReturn(projection);

        CountrySalarySummaryResponse response = reportingService.getCountrySalarySummary("US", null);

        assertNotNull(response);
        assertEquals(LocalDate.now(), response.getAsOfDate());
        assertEquals(1L, response.getEligibleEmployeeCount());
        assertEquals(new BigDecimal("60000.00"), response.getAverageSalary());
        verify(compensationRecordRepository).getCountrySalarySummary("US", LocalDate.now());
    }

    @Test
    @DisplayName("should filter salary summary by job title when jobTitle is provided")
    void shouldFilterSalarySummaryByJobTitleWhenJobTitleProvided() {
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);
        CountrySalarySummaryProjection projection = mock(CountrySalarySummaryProjection.class);
        when(projection.getEmployeeCount()).thenReturn(2L);
        when(projection.getMinSalary()).thenReturn(new BigDecimal("70000.00"));
        when(projection.getMaxSalary()).thenReturn(new BigDecimal("90000.00"));
        when(projection.getAverageSalary()).thenReturn(80000.00);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getCountrySalarySummaryByJobTitle("US", asOfDate, "Software Engineer"))
                .thenReturn(projection);

        CountrySalarySummaryResponse response = reportingService.getCountrySalarySummary("US", asOfDate, "Software Engineer");

        assertNotNull(response);
        assertEquals(2L, response.getEligibleEmployeeCount());
        assertEquals(new BigDecimal("70000.00"), response.getMinSalary());
        assertEquals(new BigDecimal("90000.00"), response.getMaxSalary());
        assertEquals(new BigDecimal("80000.00"), response.getAverageSalary());
        verify(compensationRecordRepository).getCountrySalarySummaryByJobTitle("US", asOfDate, "Software Engineer");
    }

    @Test
    @DisplayName("should use country-wide summary when jobTitle is blank")
    void shouldUseCountryWideSummaryWhenJobTitleIsBlank() {
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);
        CountrySalarySummaryProjection projection = mock(CountrySalarySummaryProjection.class);
        when(projection.getEmployeeCount()).thenReturn(5L);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getCountrySalarySummary("US", asOfDate)).thenReturn(projection);

        CountrySalarySummaryResponse response = reportingService.getCountrySalarySummary("US", asOfDate, "   ");

        assertNotNull(response);
        assertEquals(5L, response.getEligibleEmployeeCount());
        verify(compensationRecordRepository).getCountrySalarySummary("US", asOfDate);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when country is unknown")
    void shouldThrowResourceNotFoundExceptionWhenCountryUnknown() {
        when(countryRepository.findById("XX")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reportingService.getCountrySalarySummary("XX", LocalDate.of(2025, 1, 1)));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when country code is blank or null")
    void shouldThrowResourceNotFoundExceptionWhenCountryCodeIsBlank() {
        assertThrows(ResourceNotFoundException.class, () ->
                reportingService.getCountrySalarySummary("  ", LocalDate.of(2025, 1, 1)));
        assertThrows(ResourceNotFoundException.class, () ->
                reportingService.getCountrySalarySummary(null, LocalDate.of(2025, 1, 1)));
    }

    @Test
    @DisplayName("should return grouped salaries by job title within country")
    void shouldReturnGroupedSalariesByJobTitle() {
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);

        JobTitleSalarySummaryProjection p1 = mock(JobTitleSalarySummaryProjection.class);
        when(p1.getJobTitle()).thenReturn("Backend Engineer");
        when(p1.getEmployeeCount()).thenReturn(2L);
        when(p1.getAverageSalary()).thenReturn(85000.50);

        JobTitleSalarySummaryProjection p2 = mock(JobTitleSalarySummaryProjection.class);
        when(p2.getJobTitle()).thenReturn("Engineering Manager");
        when(p2.getEmployeeCount()).thenReturn(1L);
        when(p2.getAverageSalary()).thenReturn(120000.00);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getSalaryByJobTitle("US", asOfDate)).thenReturn(List.of(p1, p2));

        CountryJobTitleSalaryResponse response = reportingService.getSalaryByJobTitle("US", asOfDate, null);

        assertNotNull(response);
        assertEquals("US", response.getCountryCode());
        assertEquals("United States", response.getCountryName());
        assertEquals("USD", response.getCurrencyCode());
        assertEquals(asOfDate, response.getAsOfDate());
        assertEquals(3L, response.getTotalEligibleEmployees());
        assertEquals(2, response.getJobTitles().size());

        assertEquals("Backend Engineer", response.getJobTitles().get(0).getJobTitle());
        assertEquals(2L, response.getJobTitles().get(0).getEmployeeCount());
        assertEquals(new BigDecimal("85000.50"), response.getJobTitles().get(0).getAverageSalary());

        assertEquals("Engineering Manager", response.getJobTitles().get(1).getJobTitle());
        assertEquals(1L, response.getJobTitles().get(1).getEmployeeCount());
        assertEquals(new BigDecimal("120000.00"), response.getJobTitles().get(1).getAverageSalary());
    }

    @Test
    @DisplayName("should filter by specific job title when jobTitle parameter is supplied")
    void shouldFilterBySpecificJobTitle() {
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);

        JobTitleSalarySummaryProjection p1 = mock(JobTitleSalarySummaryProjection.class);
        when(p1.getJobTitle()).thenReturn("Backend Engineer");
        when(p1.getEmployeeCount()).thenReturn(2L);
        when(p1.getAverageSalary()).thenReturn(85000.50);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getSalaryByJobTitleAndCountry("US", asOfDate, "Backend Engineer"))
                .thenReturn(List.of(p1));

        CountryJobTitleSalaryResponse response = reportingService.getSalaryByJobTitle("US", asOfDate, "Backend Engineer");

        assertNotNull(response);
        assertEquals(1, response.getJobTitles().size());
        assertEquals("Backend Engineer", response.getJobTitles().get(0).getJobTitle());
        assertEquals(2L, response.getTotalEligibleEmployees());
        verify(compensationRecordRepository).getSalaryByJobTitleAndCountry("US", asOfDate, "Backend Engineer");
    }

    @Test
    @DisplayName("should return empty list and zero count when no eligible employees match job title query")
    void shouldReturnEmptyJobTitlesWhenNoneEligible() {
        LocalDate asOfDate = LocalDate.of(2025, 6, 1);

        when(countryRepository.findById("US")).thenReturn(Optional.of(countryUS));
        when(compensationRecordRepository.getSalaryByJobTitle("US", asOfDate)).thenReturn(List.of());

        CountryJobTitleSalaryResponse response = reportingService.getSalaryByJobTitle("US", asOfDate, null);

        assertNotNull(response);
        assertEquals(0L, response.getTotalEligibleEmployees());
        assertEquals(0, response.getJobTitles().size());
    }
}
