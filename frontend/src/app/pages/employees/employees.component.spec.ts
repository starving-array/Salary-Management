import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
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
