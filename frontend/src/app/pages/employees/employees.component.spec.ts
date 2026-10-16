import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ApiErrorService } from '../../core/services/api-error.service';
import { CompensationApiService } from '../../core/services/compensation-api.service';
import { EmployeeApiService } from '../../core/services/employee-api.service';
import { ApiError } from '../../models/api-error.model';
import { Employee, EmployeePageResponse } from '../../models/employee.model';
import { EmployeesComponent } from './employees.component';

describe('EmployeesComponent', () => {
  let component: EmployeesComponent;
  let fixture: ComponentFixture<EmployeesComponent>;
  let employeeApiService: {
    getEmployees: ReturnType<typeof vi.fn>;
    getJobTitles: ReturnType<typeof vi.fn>;
    createEmployee: ReturnType<typeof vi.fn>;
    updateEmployee: ReturnType<typeof vi.fn>;
    deleteEmployee: ReturnType<typeof vi.fn>;
  };

  const mockEmployee1: Employee = {
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

  const mockEmployee2: Employee = {
    id: '22222222-2222-2222-2222-222222222222',
    employeeNumber: 'EMP-002',
    firstName: 'Alan',
    lastName: 'Turing',
    email: 'alan@example.com',
    countryCode: 'GB',
    jobTitle: 'Cryptanalyst',
    department: 'Computing',
    joinedAt: '2024-02-01',
    terminatedAt: '2025-01-01'
  };

  const mockPageResponse: EmployeePageResponse = {
    content: [mockEmployee1, mockEmployee2],
    totalElements: 2,
    totalPages: 1,
    page: 0,
    size: 20,
    hasNext: false
  };

  beforeEach(async () => {
    employeeApiService = {
      getEmployees: vi.fn().mockReturnValue(of(mockPageResponse)),
      getJobTitles: vi.fn().mockReturnValue(of(['Software Engineer', 'Senior Software Engineer'])),
      createEmployee: vi.fn(),
      updateEmployee: vi.fn(),
      deleteEmployee: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [EmployeesComponent],
      providers: [
        { provide: EmployeeApiService, useValue: employeeApiService },
        {
          provide: CompensationApiService,
          useValue: {
            getCompensationHistory: vi.fn().mockReturnValue(of([])),
            assignInitialSalary: vi.fn(),
            recordSalaryChange: vi.fn()
          }
        },
        ApiErrorService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeesComponent);
    component = fixture.componentInstance;
  });

  it('should create and load initial employee directory', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(employeeApiService.getEmployees).toHaveBeenCalledWith({
      search: '',
      countryCode: '',
      jobTitle: '',
      page: 0,
      size: 20
    });
    expect(component.employees.length).toBe(2);
    expect(component.employees[0].employeeNumber).toBe('EMP-001');
  });

  describe('Directory Rendering and States', () => {
    it('should render table rows for loaded employees', () => {
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const rows = compiled.querySelectorAll('tbody tr');
      expect(rows.length).toBe(2);

      const firstRowText = rows[0].textContent;
      expect(firstRowText).toContain('EMP-001');
      expect(firstRowText).toContain('Ada Lovelace');
      expect(firstRowText).toContain('ada@example.com');
      expect(firstRowText).toContain('Lead Mathematician');
    });

    it('should show empty state when directory is empty', () => {
      employeeApiService.getEmployees.mockReturnValue(of({
        content: [],
        totalElements: 0,
        totalPages: 0,
        page: 0,
        size: 20,
        hasNext: false
      }));

      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const emptyCard = compiled.querySelector('.empty-state-card');
      expect(emptyCard).toBeTruthy();
      expect(emptyCard?.textContent).toContain('No employees found');
    });

    it('should show error banner and allow retry on API failure', () => {
      employeeApiService.getEmployees.mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 500,
        statusText: 'Internal Server Error',
        error: { message: 'Internal server error' }
      })));

      fixture.detectChanges();

      expect(component.directoryError).toBe('Internal server error');
      const compiled = fixture.nativeElement as HTMLElement;
      const errorBanner = compiled.querySelector('.error-banner');
      expect(errorBanner).toBeTruthy();
      expect(errorBanner?.textContent).toContain('Internal server error');

      // Click retry
      employeeApiService.getEmployees.mockReturnValue(of(mockPageResponse));
      const retryBtn = errorBanner?.querySelector('button') as HTMLButtonElement;
      retryBtn.click();
      fixture.detectChanges();

      expect(component.directoryError).toBeNull();
      expect(component.employees.length).toBe(2);
    });

    it('should show loading indicator while API request is in-flight and clear it upon successful response', () => {
      const response$ = new Subject<EmployeePageResponse>();
      employeeApiService.getEmployees.mockReturnValue(response$);

      const asyncFixture = TestBed.createComponent(EmployeesComponent);
      asyncFixture.detectChanges();

      expect(asyncFixture.componentInstance.loading).toBe(true);
      const compiled = asyncFixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.loading-container')).toBeTruthy();
      expect(compiled.querySelector('.loading-text')?.textContent).toContain('Loading employee records...');

      // Emit successful paginated response
      response$.next(mockPageResponse);
      response$.complete();
      asyncFixture.detectChanges();

      expect(asyncFixture.componentInstance.loading).toBe(false);
      expect(compiled.querySelector('.loading-container')).toBeNull();
      expect(compiled.querySelectorAll('tbody tr').length).toBe(2);
      expect(asyncFixture.componentInstance.employees.length).toBe(2);
    });

    it('should clear loading indicator and display error banner when API returns an error', () => {
      const response$ = new Subject<EmployeePageResponse>();
      employeeApiService.getEmployees.mockReturnValue(response$);

      const asyncFixture = TestBed.createComponent(EmployeesComponent);
      asyncFixture.detectChanges();

      expect(asyncFixture.componentInstance.loading).toBe(true);
      const compiled = asyncFixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.loading-container')).toBeTruthy();

      // Emit error
      response$.error(new HttpErrorResponse({
        status: 503,
        statusText: 'Service Unavailable',
        error: { message: 'Service temporarily unavailable' }
      }));
      asyncFixture.detectChanges();

      expect(asyncFixture.componentInstance.loading).toBe(false);
      expect(compiled.querySelector('.loading-container')).toBeNull();
      expect(asyncFixture.componentInstance.directoryError).toBe('Service temporarily unavailable');
      expect(compiled.querySelector('.error-banner')?.textContent).toContain('Service temporarily unavailable');
    });
  });

  describe('Search and Filter Controls', () => {
    it('should debounce search input and trigger server search', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      employeeApiService.getEmployees.mockClear();

      component.searchControl.setValue('Ada');
      // Within debounce period, no request yet
      vi.advanceTimersByTime(100);
      expect(employeeApiService.getEmployees).not.toHaveBeenCalled();

      // After 300ms debounce
      vi.advanceTimersByTime(200);
      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        search: 'Ada',
        page: 0
      }));
      vi.useRealTimers();
    });

    it('should discard rapid consecutive search keystrokes and only send latest', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      employeeApiService.getEmployees.mockClear();

      component.searchControl.setValue('A');
      vi.advanceTimersByTime(100);
      component.searchControl.setValue('Ad');
      vi.advanceTimersByTime(100);
      component.searchControl.setValue('Ada');
      vi.advanceTimersByTime(300);

      expect(employeeApiService.getEmployees).toHaveBeenCalledTimes(1);
      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        search: 'Ada',
        page: 0
      }));
      vi.useRealTimers();
    });

    it('should filter by country code', () => {
      fixture.detectChanges();
      employeeApiService.getEmployees.mockClear();

      component.countryControl.setValue('GB');
      fixture.detectChanges();

      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        countryCode: 'GB',
        page: 0
      }));
    });

    it('should debounce job title filter', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      employeeApiService.getEmployees.mockClear();

      component.jobTitleControl.setValue('Engineer');
      vi.advanceTimersByTime(300);

      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        jobTitle: 'Engineer',
        page: 0
      }));
      vi.useRealTimers();
    });

    it('should reset all filters and reload', () => {
      fixture.detectChanges();
      component.searchControl.setValue('test');
      component.countryControl.setValue('US');
      component.jobTitleControl.setValue('dev');

      employeeApiService.getEmployees.mockClear();
      component.resetFilters();
      fixture.detectChanges();

      expect(component.searchControl.value).toBe('');
      expect(component.countryControl.value).toBe('');
      expect(component.jobTitleControl.value).toBe('');
      expect(employeeApiService.getEmployees).toHaveBeenCalledWith({
        search: '',
        countryCode: '',
        jobTitle: '',
        page: 0,
        size: 20
      });
    });
  });

  describe('Job Title Autocomplete', () => {
    it('should not query autocomplete suggestions if typed input is less than 2 characters', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      employeeApiService.getJobTitles.mockClear();

      component.jobTitleControl.setValue('a');
      vi.advanceTimersByTime(350);

      expect(employeeApiService.getJobTitles).not.toHaveBeenCalled();
      expect(component.showJobTitleSuggestions).toBe(false);
      expect(component.jobTitleSuggestions).toEqual([]);
      vi.useRealTimers();
    });

    it('should debounce for 300ms and query suggestions matching job titles (max 10)', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      employeeApiService.getJobTitles.mockClear();

      const mockTitles = ['Software Engineer', 'Senior Software Engineer', 'Staff Software Engineer'];
      employeeApiService.getJobTitles.mockReturnValue(of(mockTitles));

      component.jobTitleControl.setValue('soft');
      vi.advanceTimersByTime(150);
      expect(employeeApiService.getJobTitles).not.toHaveBeenCalled();

      vi.advanceTimersByTime(150);
      expect(employeeApiService.getJobTitles).toHaveBeenCalledWith('soft', 10);
      expect(component.showJobTitleSuggestions).toBe(true);
      expect(component.jobTitleSuggestions).toEqual(mockTitles);
      expect(component.activeSuggestionIndex).toBe(-1);
      vi.useRealTimers();
    });

    it('should display suggestions in listbox with accessible attributes', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      const mockTitles = ['Software Engineer', 'Senior Software Engineer'];
      employeeApiService.getJobTitles.mockReturnValue(of(mockTitles));

      component.jobTitleControl.setValue('soft');
      vi.advanceTimersByTime(300);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const input = compiled.querySelector('#job-title-filter') as HTMLInputElement;
      expect(input.getAttribute('role')).toBe('combobox');
      expect(input.getAttribute('aria-autocomplete')).toBe('list');
      expect(input.getAttribute('aria-expanded')).toBe('true');
      expect(input.getAttribute('aria-controls')).toBe('job-title-listbox');

      const listbox = compiled.querySelector('#job-title-listbox');
      expect(listbox).not.toBeNull();
      expect(listbox?.getAttribute('role')).toBe('listbox');

      const items = compiled.querySelectorAll('.autocomplete-item');
      expect(items.length).toBe(2);
      expect(items[0].textContent?.trim()).toBe('Software Engineer');
      expect(items[1].textContent?.trim()).toBe('Senior Software Engineer');
      expect(items[0].getAttribute('role')).toBe('option');
      vi.useRealTimers();
    });

    it('should display clear empty-suggestions state when no titles match', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      employeeApiService.getJobTitles.mockReturnValue(of([]));

      component.jobTitleControl.setValue('unknownrole');
      vi.advanceTimersByTime(300);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const emptyItem = compiled.querySelector('.autocomplete-empty');
      expect(emptyItem).not.toBeNull();
      expect(emptyItem?.textContent?.trim()).toBe('No matching job titles');
      vi.useRealTimers();
    });

    it('should support keyboard navigation: ArrowDown, ArrowUp, Enter, and Escape', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      const mockTitles = ['Software Engineer', 'Senior Software Engineer'];
      employeeApiService.getJobTitles.mockReturnValue(of(mockTitles));

      component.jobTitleControl.setValue('soft');
      vi.advanceTimersByTime(300);
      fixture.detectChanges();

      expect(component.showJobTitleSuggestions).toBe(true);
      expect(component.activeSuggestionIndex).toBe(-1);

      // ArrowDown -> first item (index 0)
      component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
      expect(component.activeSuggestionIndex).toBe(0);
      expect(component.activeSuggestionId).toBe('job-title-option-0');

      // ArrowDown -> second item (index 1)
      component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
      expect(component.activeSuggestionIndex).toBe(1);
      expect(component.activeSuggestionId).toBe('job-title-option-1');

      // ArrowDown wraps to index 0
      component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
      expect(component.activeSuggestionIndex).toBe(0);

      // ArrowUp wraps back to index 1
      component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
      expect(component.activeSuggestionIndex).toBe(1);

      // Enter selects active suggestion
      employeeApiService.getEmployees.mockClear();
      component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'Enter' }));

      expect(component.jobTitleControl.value).toBe('Senior Software Engineer');
      expect(component.showJobTitleSuggestions).toBe(false);
      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        jobTitle: 'Senior Software Engineer',
        page: 0
      }));

      // Escape closes suggestions
      component.jobTitleSuggestions = mockTitles;
      component.showJobTitleSuggestions = true;
      component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'Escape' }));
      expect(component.showJobTitleSuggestions).toBe(false);

      vi.useRealTimers();
    });

    it('should select suggestion by mouse click and apply exact filter', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      const mockTitles = ['DevOps Engineer', 'Software Engineer'];
      employeeApiService.getJobTitles.mockReturnValue(of(mockTitles));

      component.jobTitleControl.setValue('eng');
      vi.advanceTimersByTime(300);
      fixture.detectChanges();

      employeeApiService.getEmployees.mockClear();
      employeeApiService.getJobTitles.mockClear();

      component.selectJobTitleSuggestion('DevOps Engineer');

      expect(component.jobTitleControl.value).toBe('DevOps Engineer');
      expect(component.showJobTitleSuggestions).toBe(false);
      // Ensure exact filter applied and reloads directory
      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        jobTitle: 'DevOps Engineer',
        page: 0
      }));
      // Verify no duplicate autocomplete search request is sent
      vi.advanceTimersByTime(350);
      expect(employeeApiService.getJobTitles).not.toHaveBeenCalled();

      vi.useRealTimers();
    });

    it('should clear suggestions and title filter when input is cleared', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      component.jobTitleSuggestions = ['Software Engineer'];
      component.showJobTitleSuggestions = true;

      component.jobTitleControl.setValue('');
      vi.advanceTimersByTime(300);

      expect(component.showJobTitleSuggestions).toBe(false);
      expect(component.jobTitleSuggestions).toEqual([]);
      expect(component.filterState$.getValue().jobTitle).toBe('');
      vi.useRealTimers();
    });

    it('should hide suggestions on blur after timeout', () => {
      vi.useFakeTimers();
      component.jobTitleSuggestions = ['Software Engineer'];
      component.showJobTitleSuggestions = true;

      component.onJobTitleBlur();
      expect(component.showJobTitleSuggestions).toBe(true);

      vi.advanceTimersByTime(250);
      expect(component.showJobTitleSuggestions).toBe(false);
      vi.useRealTimers();
    });
  });

  describe('Pagination', () => {
    it('should navigate to next and previous pages', () => {
      const multiPageResponse: EmployeePageResponse = {
        content: [mockEmployee1],
        totalElements: 25,
        totalPages: 2,
        page: 0,
        size: 20,
        hasNext: true
      };
      employeeApiService.getEmployees.mockReturnValue(of(multiPageResponse));
      fixture.detectChanges();

      employeeApiService.getEmployees.mockClear();
      component.goToPage(1);

      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        page: 1
      }));
    });

    it('should change page size and reset to page 0', () => {
      fixture.detectChanges();
      employeeApiService.getEmployees.mockClear();

      const event = { target: { value: '50' } } as unknown as Event;
      component.onPageSizeChange(event);

      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        page: 0,
        size: 50
      }));
    });
  });

  describe('Create Employee Workflow', () => {
    it('should open modal with empty form and validate required fields', () => {
      fixture.detectChanges();
      component.openCreateModal();

      expect(component.showCreateModal).toBe(true);
      expect(component.createForm.valid).toBe(false);

      component.submitCreate();
      expect(component.createForm.get('employeeNumber')?.touched).toBe(true);
      expect(employeeApiService.createEmployee).not.toHaveBeenCalled();
    });

    it('should validate that termination date is not before joining date', () => {
      fixture.detectChanges();
      component.openCreateModal();

      component.createForm.patchValue({
        employeeNumber: 'EMP-003',
        firstName: 'Grace',
        lastName: 'Hopper',
        email: 'grace@example.com',
        countryCode: 'US',
        jobTitle: 'Admiral',
        joinedAt: '2024-05-01',
        terminatedAt: '2024-04-30'
      });

      expect(component.createForm.errors?.['terminationBeforeJoining']).toBe(true);
      expect(component.createForm.valid).toBe(false);
    });

    it('should successfully submit valid employee and refresh list', () => {
      const newEmployee: Employee = {
        id: '33333333-3333-3333-3333-333333333333',
        employeeNumber: 'EMP-003',
        firstName: 'Grace',
        lastName: 'Hopper',
        email: 'grace@example.com',
        countryCode: 'US',
        jobTitle: 'Rear Admiral',
        department: 'Computing',
        joinedAt: '2024-05-01',
        terminatedAt: null
      };

      employeeApiService.createEmployee.mockReturnValue(of(newEmployee));
      fixture.detectChanges();
      component.openCreateModal();

      component.createForm.patchValue({
        employeeNumber: 'EMP-003',
        firstName: 'Grace',
        lastName: 'Hopper',
        email: 'grace@example.com',
        countryCode: 'US',
        jobTitle: 'Rear Admiral',
        department: 'Computing',
        joinedAt: '2024-05-01'
      });

      expect(component.createForm.valid).toBe(true);

      employeeApiService.getEmployees.mockClear();
      component.submitCreate();

      expect(employeeApiService.createEmployee).toHaveBeenCalledWith({
        employeeNumber: 'EMP-003',
        firstName: 'Grace',
        lastName: 'Hopper',
        email: 'grace@example.com',
        countryCode: 'US',
        jobTitle: 'Rear Admiral',
        department: 'Computing',
        joinedAt: '2024-05-01',
        terminatedAt: null
      });

      expect(component.showCreateModal).toBe(false);
      expect(component.notificationMessage).toContain('Grace Hopper (EMP-003) created successfully');
      expect(employeeApiService.getEmployees).toHaveBeenCalled();
    });

    it('should display conflict error (409) inside modal without closing it', () => {
      const errorResponse = new HttpErrorResponse({
        status: 409,
        statusText: 'Conflict',
        error: {
          timestamp: new Date().toISOString(),
          status: 409,
          error: 'Conflict',
          message: 'Employee number already exists: EMP-001',
          details: []
        }
      });

      employeeApiService.createEmployee.mockReturnValue(throwError(() => errorResponse));
      fixture.detectChanges();
      component.openCreateModal();

      component.createForm.patchValue({
        employeeNumber: 'EMP-001',
        firstName: 'Duplicate',
        lastName: 'Employee',
        email: 'dup@example.com',
        countryCode: 'US',
        jobTitle: 'Engineer',
        joinedAt: '2024-01-01'
      });

      component.submitCreate();

      expect(component.showCreateModal).toBe(true);
      expect(component.createFormError).toBe('Employee number already exists: EMP-001');
      expect(component.savingCreate).toBe(false);
    });
  });

  describe('Edit Employee Workflow', () => {
    it('should open edit modal with prepopulated values and immutable context', () => {
      fixture.detectChanges();
      component.openEditModal(mockEmployee1);

      expect(component.showEditModal).toBe(true);
      expect(component.selectedEmployee).toBe(mockEmployee1);
      expect(component.editForm.get('firstName')?.value).toBe('Ada');
      expect(component.editForm.get('lastName')?.value).toBe('Lovelace');
      expect(component.editForm.get('email')?.value).toBe('ada@example.com');
      expect(component.editForm.get('jobTitle')?.value).toBe('Lead Mathematician');
    });

    it('should submit updated details and preserve original country code', () => {
      const updatedEmployee: Employee = {
        ...mockEmployee1,
        firstName: 'Ada',
        lastName: 'King',
        jobTitle: 'Senior Mathematician'
      };

      employeeApiService.updateEmployee.mockReturnValue(of(updatedEmployee));
      fixture.detectChanges();
      component.openEditModal(mockEmployee1);

      component.editForm.patchValue({
        firstName: 'Ada',
        lastName: 'King',
        email: 'ada@example.com',
        jobTitle: 'Senior Mathematician',
        department: 'Analytical Engine'
      });

      employeeApiService.getEmployees.mockClear();
      component.submitEdit();

      expect(employeeApiService.updateEmployee).toHaveBeenCalledWith(mockEmployee1.id, {
        firstName: 'Ada',
        lastName: 'King',
        email: 'ada@example.com',
        countryCode: 'US', // original country code preserved
        jobTitle: 'Senior Mathematician',
        department: 'Analytical Engine',
        terminatedAt: null
      });

      expect(component.showEditModal).toBe(false);
      expect(component.notificationMessage).toContain('Ada King updated successfully');
      expect(employeeApiService.getEmployees).toHaveBeenCalled();
    });

    it('should display error when edit update fails', () => {
      employeeApiService.updateEmployee.mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 409,
        statusText: 'Conflict',
        error: {
          status: 409,
          message: 'Email already exists: ada@example.com'
        }
      })));

      fixture.detectChanges();
      component.openEditModal(mockEmployee1);

      component.submitEdit();

      expect(component.showEditModal).toBe(true);
      expect(component.editFormError).toBe('Email already exists: ada@example.com');
      expect(component.savingEdit).toBe(false);
    });
  });

  describe('Delete Workflow', () => {
    it('should open delete confirmation modal and cancel cleanly', () => {
      fixture.detectChanges();
      component.openDeleteModal(mockEmployee1);

      expect(component.showDeleteModal).toBe(true);
      expect(component.employeeToDelete).toBe(mockEmployee1);

      component.closeDeleteModal();
      expect(component.showDeleteModal).toBe(false);
      expect(component.employeeToDelete).toBeNull();
      expect(employeeApiService.deleteEmployee).not.toHaveBeenCalled();
    });

    it('should delete employee and refresh list on confirmation', () => {
      employeeApiService.deleteEmployee.mockReturnValue(of(undefined));
      fixture.detectChanges();
      component.openDeleteModal(mockEmployee1);

      employeeApiService.getEmployees.mockClear();
      component.confirmDelete();

      expect(employeeApiService.deleteEmployee).toHaveBeenCalledWith(mockEmployee1.id);
      expect(component.showDeleteModal).toBe(false);
      expect(component.notificationMessage).toContain('Ada Lovelace (EMP-001) was removed');
      expect(employeeApiService.getEmployees).toHaveBeenCalled();
    });

    it('should step back one page if last item on non-first page was deleted', () => {
      const singleItemPage: EmployeePageResponse = {
        content: [mockEmployee2],
        totalElements: 21,
        totalPages: 2,
        page: 1,
        size: 20,
        hasNext: false
      };
      employeeApiService.getEmployees.mockReturnValue(of(singleItemPage));
      component.goToPage(1);
      fixture.detectChanges();

      expect(component.employees.length).toBe(1);
      expect(component.filterState$.getValue().page).toBe(1);

      employeeApiService.deleteEmployee.mockReturnValue(of(undefined));
      component.openDeleteModal(mockEmployee2);

      employeeApiService.getEmployees.mockClear();
      component.confirmDelete();

      expect(component.filterState$.getValue().page).toBe(0);
      expect(employeeApiService.getEmployees).toHaveBeenCalledWith(expect.objectContaining({
        page: 0
      }));
    });

    it('should handle 404 gracefully if employee was already deleted', () => {
      employeeApiService.deleteEmployee.mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 404,
        statusText: 'Not Found',
        error: {
          status: 404,
          message: 'Employee not found'
        }
      })));

      fixture.detectChanges();
      component.openDeleteModal(mockEmployee1);

      component.confirmDelete();

      expect(component.showDeleteModal).toBe(false);
      expect(component.notificationMessage).toContain('is no longer in the active directory');
    });
  });

  describe('Accessibility and Keyboard Handling', () => {
    it('should close open modal on Escape key press', () => {
      fixture.detectChanges();

      // Open create modal
      component.openCreateModal();
      expect(component.showCreateModal).toBe(true);

      // Press Escape
      component.onEscapePressed();
      expect(component.showCreateModal).toBe(false);

      // Open edit modal
      component.openEditModal(mockEmployee1);
      expect(component.showEditModal).toBe(true);

      component.onEscapePressed();
      expect(component.showEditModal).toBe(false);

      // Open delete modal
      component.openDeleteModal(mockEmployee1);
      expect(component.showDeleteModal).toBe(true);

      component.onEscapePressed();
      expect(component.showDeleteModal).toBe(false);

      // Open compensation modal
      component.openCompensationModal(mockEmployee1);
      expect(component.selectedCompensationEmployee).toBe(mockEmployee1);

      component.onEscapePressed();
      expect(component.selectedCompensationEmployee).toBeNull();
    });

    it('should open compensation modal when clicking Comp button in directory row', () => {
      fixture.detectChanges();

      const compBtn = fixture.nativeElement.querySelector('.btn-comp') as HTMLButtonElement;
      expect(compBtn).toBeTruthy();

      compBtn.click();
      fixture.detectChanges();

      expect(component.selectedCompensationEmployee).toBeTruthy();
      expect(component.selectedCompensationEmployee?.employeeNumber).toBe('EMP-001');

      component.closeCompensationModal();
      expect(component.selectedCompensationEmployee).toBeNull();
    });

    it('should have proper ARIA attributes on modal dialogs', () => {
      fixture.detectChanges();
      const addBtn = fixture.nativeElement.querySelector('.btn-primary') as HTMLButtonElement;
      addBtn.click();
      fixture.detectChanges();

      expect(component.showCreateModal).toBe(true);
      const modalDialog = fixture.nativeElement.querySelector('[role="dialog"]');
      expect(modalDialog).toBeTruthy();
      expect(modalDialog?.getAttribute('aria-modal')).toBe('true');
      expect(modalDialog?.getAttribute('aria-labelledby')).toBe('create-modal-title');
    });
  });
});
