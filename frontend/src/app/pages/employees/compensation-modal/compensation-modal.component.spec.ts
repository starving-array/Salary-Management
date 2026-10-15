import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { CompensationApiService } from '../../../core/services/compensation-api.service';
import { CompensationRecord } from '../../../models/compensation.model';
import { Employee } from '../../../models/employee.model';
import { CompensationModalComponent } from './compensation-modal.component';

describe('CompensationModalComponent', () => {
  let component: CompensationModalComponent;
  let fixture: ComponentFixture<CompensationModalComponent>;
  let compensationApiService: {
    getCompensationHistory: ReturnType<typeof vi.fn>;
    assignInitialSalary: ReturnType<typeof vi.fn>;
    recordSalaryChange: ReturnType<typeof vi.fn>;
  };

  const mockEmployee: Employee = {
    id: '11111111-1111-1111-1111-111111111111',
    employeeNumber: 'EMP-001',
    firstName: 'Ada',
    lastName: 'Lovelace',
    email: 'ada@example.com',
    countryCode: 'US',
    jobTitle: 'Lead Mathematician',
    department: 'Analytical Engine',
    joinedAt: '2024-01-15',
    terminatedAt: null
  };

  const mockRecord1: CompensationRecord = {
    id: 'c1111111-1111-1111-1111-111111111111',
    employeeId: mockEmployee.id,
    annualSalary: 100000,
    currencyCode: 'USD',
    effectiveFrom: '2024-01-15',
    effectiveTo: '2024-07-01',
    createdAt: '2024-01-15T10:00:00Z'
  };

  const mockRecord2: CompensationRecord = {
    id: 'c2222222-2222-2222-2222-222222222222',
    employeeId: mockEmployee.id,
    annualSalary: 120000,
    currencyCode: 'USD',
    effectiveFrom: '2024-07-01',
    effectiveTo: null,
    createdAt: '2024-07-01T10:00:00Z'
  };

  beforeEach(async () => {
    compensationApiService = {
      getCompensationHistory: vi.fn().mockReturnValue(of([mockRecord1, mockRecord2])),
      assignInitialSalary: vi.fn(),
      recordSalaryChange: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [CompensationModalComponent],
      providers: [
        { provide: CompensationApiService, useValue: compensationApiService },
        ApiErrorService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CompensationModalComponent);
    component = fixture.componentInstance;
    component.employee = mockEmployee;
  });

  it('should create and load compensation history on init', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(compensationApiService.getCompensationHistory).toHaveBeenCalledWith(mockEmployee.id);
    expect(component.records.length).toBe(2);
    expect(component.hasHistory).toBe(true);
  });

  describe('History Display and Statuses', () => {
    it('should display employee summary profile', () => {
      fixture.detectChanges();

      const text = fixture.nativeElement.textContent;
      expect(text).toContain('Ada Lovelace');
      expect(text).toContain('EMP-001');
      expect(text).toContain('USD');
    });

    it('should render chronological table with current and historical records', () => {
      fixture.detectChanges();

      const rows = fixture.nativeElement.querySelectorAll('tbody tr');
      expect(rows.length).toBe(2);

      // Record 1 (Historical)
      expect(rows[0].textContent).toContain('Historical');
      expect(rows[0].textContent).toContain('$100,000.00');

      // Record 2 (Current, Open-ended)
      expect(rows[1].textContent).toContain('Current');
      expect(rows[1].textContent).toContain('$120,000.00');
      expect(rows[1].textContent).toContain('Present (Active)');
    });
  });

  describe('Empty History and API Failure States', () => {
    it('should display empty history state and initial salary form when no records exist', () => {
      compensationApiService.getCompensationHistory.mockReturnValue(of([]));
      fixture.detectChanges();

      expect(component.hasHistory).toBe(false);
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.empty-history-box')).toBeTruthy();
      expect(compiled.querySelector('.initial-salary-form')).toBeTruthy();
      expect(compiled.querySelector('.data-table')).toBeFalsy();
    });

    it('should display error banner and allow retry on API failure', () => {
      compensationApiService.getCompensationHistory.mockReturnValue(
        throwError(() => new HttpErrorResponse({
          status: 500,
          statusText: 'Internal Server Error',
          error: { message: 'Database connection failed' }
        }))
      );

      fixture.detectChanges();

      expect(component.historyError).toBe('Database connection failed');
      const errorBanner = fixture.nativeElement.querySelector('.error-banner');
      expect(errorBanner).toBeTruthy();
      expect(errorBanner.textContent).toContain('Database connection failed');

      // Retry
      compensationApiService.getCompensationHistory.mockReturnValue(of([mockRecord1]));
      const retryBtn = errorBanner.querySelector('button') as HTMLButtonElement;
      retryBtn.click();
      fixture.detectChanges();

      expect(component.historyError).toBeNull();
      expect(component.records.length).toBe(1);
    });

    it('should display friendly error when employee is not found', () => {
      compensationApiService.getCompensationHistory.mockReturnValue(
        throwError(() => new HttpErrorResponse({
          status: 404,
          statusText: 'Not Found',
          error: { status: 404, message: 'Employee not found' }
        }))
      );

      fixture.detectChanges();

      expect(component.historyError).toContain('Employee not found');
    });
  });

  describe('Initial Salary Workflow', () => {
    beforeEach(() => {
      compensationApiService.getCompensationHistory.mockReturnValue(of([]));
      fixture.detectChanges();
    });

    it('should prefill effectiveFrom with employee joinedAt', () => {
      expect(component.initialForm.get('effectiveFrom')?.value).toBe(mockEmployee.joinedAt);
    });

    it('should validate annualSalary is required and positive', () => {
      component.initialForm.patchValue({ annualSalary: null });
      expect(component.initialForm.valid).toBe(false);

      component.initialForm.patchValue({ annualSalary: -500 });
      expect(component.initialForm.valid).toBe(false);

      component.initialForm.patchValue({ annualSalary: 80000 });
      expect(component.initialForm.valid).toBe(true);
    });

    it('should submit initial salary and refresh history on success', () => {
      const createdRecord: CompensationRecord = {
        id: 'new-id',
        employeeId: mockEmployee.id,
        annualSalary: 85000,
        currencyCode: 'USD',
        effectiveFrom: '2024-01-15',
        effectiveTo: null,
        createdAt: '2024-01-15T12:00:00Z'
      };

      compensationApiService.assignInitialSalary.mockReturnValue(of(createdRecord));
      let updatedEmitted = false;
      component.compensationUpdated.subscribe(() => updatedEmitted = true);

      component.initialForm.patchValue({
        annualSalary: 85000,
        effectiveFrom: '2024-01-15'
      });

      compensationApiService.getCompensationHistory.mockClear();
      component.submitInitialSalary();

      expect(compensationApiService.assignInitialSalary).toHaveBeenCalledWith(mockEmployee.id, {
        annualSalary: 85000,
        effectiveFrom: '2024-01-15'
      });
      expect(updatedEmitted).toBe(true);
      expect(component.notificationMessage).toContain('Initial compensation assigned successfully');
      expect(compensationApiService.getCompensationHistory).toHaveBeenCalled();
    });

    it('should display error when initial salary submission fails', () => {
      compensationApiService.assignInitialSalary.mockReturnValue(
        throwError(() => new HttpErrorResponse({
          status: 400,
          statusText: 'Bad Request',
          error: { message: 'Compensation effective date cannot be before employee join date' }
        }))
      );

      component.initialForm.patchValue({
        annualSalary: 75000,
        effectiveFrom: '2023-01-01'
      });

      component.submitInitialSalary();

      expect(component.initialError).toBe('Compensation effective date cannot be before employee join date');
      expect(component.savingInitial).toBe(false);
    });
  });

  describe('Salary Change Workflow', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should validate new annualSalary and effectiveFrom are required', () => {
      component.changeForm.patchValue({ annualSalary: null, effectiveFrom: '' });
      expect(component.changeForm.valid).toBe(false);

      component.changeForm.patchValue({ annualSalary: 135000, effectiveFrom: '2025-01-01' });
      expect(component.changeForm.valid).toBe(true);
    });

    it('should submit salary change and refresh history', () => {
      const updatedRecord: CompensationRecord = {
        id: 'c3333333-3333-3333-3333-333333333333',
        employeeId: mockEmployee.id,
        annualSalary: 135000,
        currencyCode: 'USD',
        effectiveFrom: '2025-01-01',
        effectiveTo: null,
        createdAt: '2025-01-01T10:00:00Z'
      };

      compensationApiService.recordSalaryChange.mockReturnValue(of(updatedRecord));
      let updatedEmitted = false;
      component.compensationUpdated.subscribe(() => updatedEmitted = true);

      component.changeForm.patchValue({
        annualSalary: 135000,
        effectiveFrom: '2025-01-01'
      });

      compensationApiService.getCompensationHistory.mockClear();
      component.submitSalaryChange();

      expect(compensationApiService.recordSalaryChange).toHaveBeenCalledWith(mockEmployee.id, {
        annualSalary: 135000,
        effectiveFrom: '2025-01-01'
      });
      expect(updatedEmitted).toBe(true);
      expect(component.notificationMessage).toContain('Salary change recorded successfully');
      expect(compensationApiService.getCompensationHistory).toHaveBeenCalled();
    });

    it('should display error message when salary change causes an invalid overlap or date error', () => {
      compensationApiService.recordSalaryChange.mockReturnValue(
        throwError(() => new HttpErrorResponse({
          status: 400,
          statusText: 'Bad Request',
          error: { message: 'Requested compensation period overlaps with an existing record' }
        }))
      );

      component.changeForm.patchValue({
        annualSalary: 130000,
        effectiveFrom: '2024-03-01'
      });

      component.submitSalaryChange();

      expect(component.changeError).toBe('Requested compensation period overlaps with an existing record');
      expect(component.savingChange).toBe(false);
    });
  });

  describe('Accessibility and Interaction', () => {
    it('should close on Escape key press when not submitting', () => {
      fixture.detectChanges();
      let closed = false;
      component.close.subscribe(() => closed = true);

      component.onEscape();
      expect(closed).toBe(true);
    });

    it('should not close on Escape key press while saving', () => {
      fixture.detectChanges();
      let closed = false;
      component.close.subscribe(() => closed = true);

      component.savingInitial = true;
      component.onEscape();
      expect(closed).toBe(false);
    });

    it('should have proper ARIA dialog attributes', () => {
      fixture.detectChanges();
      const dialog = fixture.nativeElement.querySelector('[role="dialog"]');
      expect(dialog).toBeTruthy();
      expect(dialog.getAttribute('aria-modal')).toBe('true');
      expect(dialog.getAttribute('aria-labelledby')).toBe('comp-modal-title');
    });
  });
});
