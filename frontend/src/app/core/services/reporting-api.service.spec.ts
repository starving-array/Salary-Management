import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { ReportingApiService } from './reporting-api.service';
import {
  CountryJobTitleSalarySummary,
  CountrySalarySummary
} from '../../models/reporting.model';

describe('ReportingApiService', () => {
  let service: ReportingApiService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/v1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ReportingApiService,
        { provide: API_BASE_URL, useValue: baseUrl }
      ]
    });

    service = TestBed.inject(ReportingApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch country salary summary with asOfDate', () => {
    const mockSummary: CountrySalarySummary = {
      countryCode: 'US',
      countryName: 'United States',
      currencyCode: 'USD',
      asOfDate: '2025-06-01',
      eligibleEmployeeCount: 3,
      minSalary: 60000.00,
      maxSalary: 100000.00,
      averageSalary: 80000.00
    };

    service.getCountrySalarySummary('US', '2025-06-01').subscribe(summary => {
      expect(summary.countryCode).toBe('US');
      expect(summary.eligibleEmployeeCount).toBe(3);
      expect(summary.averageSalary).toBe(80000.00);
    });

    const req = httpMock.expectOne(`${baseUrl}/reports/countries/US/salary-summary?asOfDate=2025-06-01`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('should fetch country salary summary without asOfDate', () => {
    service.getCountrySalarySummary('US').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/reports/countries/US/salary-summary`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('asOfDate')).toBe(false);
    req.flush({});
  });

  it('should fetch grouped salary report by job title without requiring a specific job title filter', () => {
    const mockGroupedReport: CountryJobTitleSalarySummary = {
      countryCode: 'US',
      countryName: 'United States',
      currencyCode: 'USD',
      asOfDate: '2025-06-01',
      totalEligibleEmployees: 3,
      jobTitles: [
        { jobTitle: 'Junior Engineer', employeeCount: 1, averageSalary: 50000.00 },
        { jobTitle: 'Senior Engineer', employeeCount: 2, averageSalary: 100000.00 }
      ]
    };

    service.getSalaryByJobTitle('US', '2025-06-01').subscribe(report => {
      expect(report.totalEligibleEmployees).toBe(3);
      expect(report.jobTitles.length).toBe(2);
      expect(report.jobTitles[0].jobTitle).toBe('Junior Engineer');
      expect(report.jobTitles[1].jobTitle).toBe('Senior Engineer');
    });

    const req = httpMock.expectOne(`${baseUrl}/reports/countries/US/salary-by-job-title?asOfDate=2025-06-01`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('jobTitle')).toBe(false);
    req.flush(mockGroupedReport);
  });

  it('should support optional jobTitle query filter when provided', () => {
    service.getSalaryByJobTitle('US', '2025-06-01', 'Senior Engineer').subscribe();

    const req = httpMock.expectOne(
      `${baseUrl}/reports/countries/US/salary-by-job-title?asOfDate=2025-06-01&jobTitle=Senior%20Engineer`
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('jobTitle')).toBe('Senior Engineer');
    req.flush({});
  });
});
