import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ReportingApiService } from '../../core/services/reporting-api.service';
import { ApiError } from '../../models/api-error.model';
import {
  CountryJobTitleSalarySummary,
  CountrySalarySummary
} from '../../models/reporting.model';
import { ReportsComponent } from './reports.component';

describe('ReportsComponent', () => {
  let component: ReportsComponent;
  let fixture: ComponentFixture<ReportsComponent>;
  let reportingApiService: {
    getCountrySalarySummary: ReturnType<typeof vi.fn>;
    getSalaryByJobTitle: ReturnType<typeof vi.fn>;
  };

  const mockSummaryUS: CountrySalarySummary = {
    countryCode: 'US',
    countryName: 'United States',
    currencyCode: 'USD',
    asOfDate: '2026-10-15',
    eligibleEmployeeCount: 3,
    minSalary: 80000.0,
    maxSalary: 120000.0,
    averageSalary: 96666.67
  };

  const mockSummaryZero: CountrySalarySummary = {
    countryCode: 'US',
    countryName: 'United States',
    currencyCode: 'USD',
    asOfDate: '2026-10-15',
    eligibleEmployeeCount: 0,
    minSalary: null,
    maxSalary: null,
    averageSalary: null
  };

  const mockSummaryGB: CountrySalarySummary = {
    countryCode: 'GB',
    countryName: 'United Kingdom',
    currencyCode: 'GBP',
    asOfDate: '2026-10-15',
    eligibleEmployeeCount: 2,
    minSalary: 50000.0,
    maxSalary: 75000.0,
    averageSalary: 62500.0
  };

  const mockJobTitleSummaryUS: CountryJobTitleSalarySummary = {
    countryCode: 'US',
    countryName: 'United States',
    currencyCode: 'USD',
    asOfDate: '2026-10-15',
    totalEligibleEmployees: 3,
    jobTitles: [
      { jobTitle: 'Data Engineer', employeeCount: 1, averageSalary: 80000.0 },
      { jobTitle: 'Software Engineer', employeeCount: 2, averageSalary: 105000.0 }
    ]
  };

  const mockJobTitleSummaryFiltered: CountryJobTitleSalarySummary = {
    countryCode: 'US',
    countryName: 'United States',
    currencyCode: 'USD',
    asOfDate: '2026-10-15',
    totalEligibleEmployees: 2,
    jobTitles: [
      { jobTitle: 'Software Engineer', employeeCount: 2, averageSalary: 105000.0 }
    ]
  };

  const mockJobTitleSummaryEmpty: CountryJobTitleSalarySummary = {
    countryCode: 'US',
    countryName: 'United States',
    currencyCode: 'USD',
    asOfDate: '2026-10-15',
    totalEligibleEmployees: 0,
    jobTitles: []
  };

  beforeEach(async () => {
    reportingApiService = {
      getCountrySalarySummary: vi.fn().mockReturnValue(of(mockSummaryUS)),
      getSalaryByJobTitle: vi.fn().mockReturnValue(of(mockJobTitleSummaryUS))
    };

    await TestBed.configureTestingModule({
      imports: [ReportsComponent],
      providers: [
        { provide: ReportingApiService, useValue: reportingApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsComponent);
    component = fixture.componentInstance;
  });

  it('should create reports component', () => {
    expect(component).toBeTruthy();
  });

  it('should load initial reports with default country US and today date', () => {
    fixture.detectChanges();

    expect(reportingApiService.getCountrySalarySummary).toHaveBeenCalledWith('US', expect.any(String));
    expect(reportingApiService.getSalaryByJobTitle).toHaveBeenCalledWith('US', expect.any(String), undefined);
    expect(component.summary).toEqual(mockSummaryUS);
    expect(component.jobTitleSummary).toEqual(mockJobTitleSummaryUS);
  });

  it('should render country salary summary statistics with USD currency', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const countEl = compiled.querySelector('.eligible-count-value');
    const minEl = compiled.querySelector('.min-salary-value');
    const maxEl = compiled.querySelector('.max-salary-value');
    const avgEl = compiled.querySelector('.avg-salary-value');

    expect(countEl?.textContent?.trim()).toBe('3');
    expect(minEl?.textContent).toContain('$80,000.00');
    expect(maxEl?.textContent).toContain('$120,000.00');
    expect(avgEl?.textContent).toContain('$96,666.67');
  });

  it('should display "—" instead of 0 when eligibleEmployeeCount is 0 or salaries are null', () => {
    reportingApiService.getCountrySalarySummary.mockReturnValue(of(mockSummaryZero));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const countEl = compiled.querySelector('.eligible-count-value');
    const minEl = compiled.querySelector('.min-salary-value');
    const maxEl = compiled.querySelector('.max-salary-value');
    const avgEl = compiled.querySelector('.avg-salary-value');

    expect(countEl?.textContent?.trim()).toBe('0');
    expect(minEl?.textContent?.trim()).toBe('—');
    expect(maxEl?.textContent?.trim()).toBe('—');
    expect(avgEl?.textContent?.trim()).toBe('—');

    const zeroCallout = compiled.querySelector('.zero-state-callout');
    expect(zeroCallout).toBeTruthy();
    expect(zeroCallout?.textContent).toContain('No eligible employees');
  });

  it('should format currency using GBP when country is GB', () => {
    reportingApiService.getCountrySalarySummary.mockReturnValue(of(mockSummaryGB));
    component.countryControl.setValue('GB');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const minEl = compiled.querySelector('.min-salary-value');
    expect(minEl?.textContent).toContain('£50,000.00');
  });

  it('should re-fetch reports when country changes', () => {
    fixture.detectChanges();
    reportingApiService.getCountrySalarySummary.mockClear();
    reportingApiService.getSalaryByJobTitle.mockClear();

    component.countryControl.setValue('GB');
    fixture.detectChanges();

    expect(reportingApiService.getCountrySalarySummary).toHaveBeenCalledWith('GB', expect.any(String));
    expect(reportingApiService.getSalaryByJobTitle).toHaveBeenCalledWith('GB', expect.any(String), undefined);
  });

  it('should re-fetch reports when asOfDate changes', () => {
    fixture.detectChanges();
    reportingApiService.getCountrySalarySummary.mockClear();
    reportingApiService.getSalaryByJobTitle.mockClear();

    component.asOfDateControl.setValue('2025-06-01');
    fixture.detectChanges();

    expect(reportingApiService.getCountrySalarySummary).toHaveBeenCalledWith('US', '2025-06-01');
    expect(reportingApiService.getSalaryByJobTitle).toHaveBeenCalledWith('US', '2025-06-01', undefined);
  });

  it('should re-fetch job title report with debounce when job title filter changes', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    reportingApiService.getSalaryByJobTitle.mockClear();
    reportingApiService.getSalaryByJobTitle.mockReturnValue(of(mockJobTitleSummaryFiltered));

    component.jobTitleFilterControl.setValue('Software');
    vi.advanceTimersByTime(100);
    expect(reportingApiService.getSalaryByJobTitle).not.toHaveBeenCalled();

    vi.advanceTimersByTime(250);
    expect(reportingApiService.getSalaryByJobTitle).toHaveBeenCalledWith('US', expect.any(String), 'Software');
    vi.useRealTimers();
  });

  it('should render job title breakdown table with correct columns and rows', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const rows = compiled.querySelectorAll('.job-title-row');
    expect(rows.length).toBe(2);

    const firstRowTitle = rows[0].querySelector('.job-title-name')?.textContent?.trim();
    const firstRowCount = rows[0].querySelector('.job-title-count')?.textContent?.trim();
    const firstRowAvg = rows[0].querySelector('.job-title-avg-salary')?.textContent?.trim();

    expect(firstRowTitle).toBe('Data Engineer');
    expect(firstRowCount).toBe('1');
    expect(firstRowAvg).toContain('$80,000.00');

    const secondRowTitle = rows[1].querySelector('.job-title-name')?.textContent?.trim();
    const secondRowCount = rows[1].querySelector('.job-title-count')?.textContent?.trim();
    const secondRowAvg = rows[1].querySelector('.job-title-avg-salary')?.textContent?.trim();

    expect(secondRowTitle).toBe('Software Engineer');
    expect(secondRowCount).toBe('2');
    expect(secondRowAvg).toContain('$105,000.00');
  });

  it('should render empty state when jobTitles array is empty', () => {
    reportingApiService.getSalaryByJobTitle.mockReturnValue(of(mockJobTitleSummaryEmpty));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.data-table')).toBeNull();
    const emptyCard = compiled.querySelector('.empty-state-card');
    expect(emptyCard).toBeTruthy();
    expect(emptyCard?.textContent).toContain('No Job Title Breakdown Available');
  });

  it('should clear job title filter when clear button is clicked in empty state', () => {
    reportingApiService.getSalaryByJobTitle.mockReturnValue(of(mockJobTitleSummaryEmpty));
    component.jobTitleFilterControl.setValue('Architect');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const clearBtn = compiled.querySelector('.empty-actions button') as HTMLButtonElement;
    expect(clearBtn).toBeTruthy();

    clearBtn.click();
    expect(component.jobTitleFilterControl.value).toBe('');
  });

  it('should display error banner and retry button when summary API fails', () => {
    const error: ApiError = {
      timestamp: '2026-10-15T00:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      message: 'Failed to calculate country summary'
    };
    reportingApiService.getCountrySalarySummary.mockReturnValue(throwError(() => error));

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const errorBanners = compiled.querySelectorAll('.error-banner');
    expect(errorBanners.length).toBeGreaterThan(0);
    expect(compiled.textContent).toContain('Failed to calculate country summary');
  });

  it('should retry loading summary report when retry button is clicked', () => {
    const error: ApiError = {
      timestamp: '2026-10-15T00:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      message: 'Failed to calculate country summary'
    };
    reportingApiService.getCountrySalarySummary.mockReturnValueOnce(throwError(() => error));

    fixture.detectChanges();
    expect(component.summaryError).toBe('Failed to calculate country summary');

    // On retry, succeed
    reportingApiService.getCountrySalarySummary.mockReturnValue(of(mockSummaryUS));
    const retryBtn = fixture.nativeElement.querySelector('.report-section .error-banner button') as HTMLButtonElement;
    retryBtn.click();
    fixture.detectChanges();

    expect(component.summaryError).toBeNull();
    expect(component.summary).toEqual(mockSummaryUS);
  });

  it('should display error banner and retry button when job title API fails', () => {
    const error: ApiError = {
      timestamp: '2026-10-15T00:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      message: 'Failed to retrieve job title statistics'
    };
    reportingApiService.getSalaryByJobTitle.mockReturnValue(throwError(() => error));

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(component.jobTitlesError).toBe('Failed to retrieve job title statistics');
    expect(compiled.textContent).toContain('Failed to retrieve job title statistics');
  });

  it('should retry loading job title report when job title retry button is clicked', () => {
    const error: ApiError = {
      timestamp: '2026-10-15T00:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      message: 'Failed to retrieve job title statistics'
    };
    reportingApiService.getSalaryByJobTitle.mockReturnValueOnce(throwError(() => error));

    fixture.detectChanges();
    expect(component.jobTitlesError).toBe('Failed to retrieve job title statistics');

    reportingApiService.getSalaryByJobTitle.mockReturnValue(of(mockJobTitleSummaryUS));
    const retryBtns = fixture.nativeElement.querySelectorAll('.report-section .error-banner button');
    const retryBtn = retryBtns[retryBtns.length - 1] as HTMLButtonElement;
    retryBtn.click();
    fixture.detectChanges();

    expect(component.jobTitlesError).toBeNull();
    expect(component.jobTitleSummary).toEqual(mockJobTitleSummaryUS);
  });

  it('should reset all filters and reload reports when Reset button is clicked', () => {
    fixture.detectChanges();
    component.countryControl.setValue('GB');
    component.asOfDateControl.setValue('2020-01-01');
    component.jobTitleFilterControl.setValue('Director');

    reportingApiService.getCountrySalarySummary.mockClear();
    reportingApiService.getSalaryByJobTitle.mockClear();

    component.resetFilters();

    expect(component.countryControl.value).toBe('US');
    expect(component.jobTitleFilterControl.value).toBe('');
    expect(reportingApiService.getCountrySalarySummary).toHaveBeenCalledWith('US', expect.any(String));
    expect(reportingApiService.getSalaryByJobTitle).toHaveBeenCalledWith('US', expect.any(String), undefined);
  });

  it('should trigger reports reload when Refresh button is clicked', () => {
    fixture.detectChanges();
    reportingApiService.getCountrySalarySummary.mockClear();
    reportingApiService.getSalaryByJobTitle.mockClear();

    const refreshBtn = fixture.nativeElement.querySelector('.filter-actions button') as HTMLButtonElement;
    refreshBtn.click();

    expect(reportingApiService.getCountrySalarySummary).toHaveBeenCalled();
    expect(reportingApiService.getSalaryByJobTitle).toHaveBeenCalled();
  });

  it('should provide accessible form controls and table headers', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('select#report-country-select')?.getAttribute('aria-label')).toBe('Select Country');
    expect(compiled.querySelector('input#report-as-of-date')?.getAttribute('aria-label')).toBe('Effective As of Date');
    expect(compiled.querySelector('input#report-job-title')?.getAttribute('aria-label')).toBe('Filter by job title');

    const tableHeaders = compiled.querySelectorAll('table.data-table th');
    tableHeaders.forEach(th => {
      expect(th.getAttribute('scope')).toBe('col');
    });
  });
});
