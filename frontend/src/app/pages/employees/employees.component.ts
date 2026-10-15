import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, HostListener, inject, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, combineLatest, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { ApiErrorService } from '../../core/services/api-error.service';
import { EmployeeApiService } from '../../core/services/employee-api.service';
import { ApiError } from '../../models/api-error.model';
import { SUPPORTED_COUNTRIES } from '../../models/country.model';
import {
  CreateEmployeeRequest,
  Employee,
  EmployeeFilterParams,
  EmployeePageResponse,
  UpdateEmployeeRequest
} from '../../models/employee.model';
import { CompensationModalComponent } from './compensation-modal/compensation-modal.component';

export function dateOrderValidator(control: AbstractControl): ValidationErrors | null {
  const joinedAt = control.get('joinedAt')?.value;
  const terminatedAt = control.get('terminatedAt')?.value;
  if (joinedAt && terminatedAt && terminatedAt < joinedAt) {
    return { terminationBeforeJoining: true };
  }
  return null;
}

export function editTerminationValidator(getJoinedAt: () => string | null | undefined): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const terminatedAt = control.get('terminatedAt')?.value;
    const joinedAt = getJoinedAt();
    if (joinedAt && terminatedAt && terminatedAt < joinedAt) {
      return { terminationBeforeJoining: true };
    }
    return null;
  };
}

@Component({
  selector: 'app-employees',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, CompensationModalComponent],
  templateUrl: './employees.component.html',
  styleUrl: './employees.component.css'
})
export class EmployeesComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly employeeApiService = inject(EmployeeApiService);
  private readonly apiErrorService = inject(ApiErrorService);
  private readonly destroyRef = inject(DestroyRef);

  readonly supportedCountries = SUPPORTED_COUNTRIES;
  readonly pageSizeOptions = [10, 20, 50, 100];

  // Directory filter controls
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly countryControl = new FormControl('', { nonNullable: true });
  readonly jobTitleControl = new FormControl('', { nonNullable: true });

  // Filter state & reload trigger
  readonly filterState$ = new BehaviorSubject<EmployeeFilterParams>({
    search: '',
    countryCode: '',
    jobTitle: '',
    page: 0,
    size: 20
  });
  private readonly reloadTrigger$ = new BehaviorSubject<number>(0);

  // Table state
  employees: Employee[] = [];
  pageInfo: EmployeePageResponse = {
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 20,
    hasNext: false
  };
  loading = false;
  directoryError: string | null = null;
  notificationMessage: string | null = null;
  private notificationTimer: ReturnType<typeof setTimeout> | null = null;

  // Create Modal State
  showCreateModal = false;
  savingCreate = false;
  createFormError: string | null = null;
  createFormDetails: string[] = [];
  createForm: FormGroup = this.fb.group({
    employeeNumber: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    countryCode: ['', [Validators.required]],
    jobTitle: ['', [Validators.required, Validators.maxLength(100)]],
    department: ['', [Validators.maxLength(100)]],
    joinedAt: ['', [Validators.required]],
    terminatedAt: ['']
  }, { validators: dateOrderValidator });

  // Edit Modal State
  showEditModal = false;
  savingEdit = false;
  selectedEmployee: Employee | null = null;
  editFormError: string | null = null;
  editFormDetails: string[] = [];
  editForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    jobTitle: ['', [Validators.required, Validators.maxLength(100)]],
    department: ['', [Validators.maxLength(100)]],
    terminatedAt: ['']
  }, { validators: editTerminationValidator(() => this.selectedEmployee?.joinedAt) });

  // Delete Dialog State
  showDeleteModal = false;
  deleting = false;
  employeeToDelete: Employee | null = null;
  deleteError: string | null = null;

  // Compensation Modal State
  selectedCompensationEmployee: Employee | null = null;

  openCompensationModal(employee: Employee): void {
    this.selectedCompensationEmployee = employee;
  }

  closeCompensationModal(): void {
    this.selectedCompensationEmployee = null;
  }

  ngOnInit(): void {
    this.setupFilterListeners();
    this.setupDirectoryStream();
  }

  ngOnDestroy(): void {
    if (this.notificationTimer) {
      clearTimeout(this.notificationTimer);
    }
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.selectedCompensationEmployee) {
      this.closeCompensationModal();
    } else if (this.showDeleteModal && !this.deleting) {
      this.closeDeleteModal();
    } else if (this.showEditModal && !this.savingEdit) {
      this.closeEditModal();
    } else if (this.showCreateModal && !this.savingCreate) {
      this.closeCreateModal();
    }
  }

  private setupFilterListeners(): void {
    // Debounce search input modestly (300ms) to avoid request per keystroke
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.updateFilter({ search: value.trim(), page: 0 });
    });

    // Country select filter triggers immediately on selection
    this.countryControl.valueChanges.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.updateFilter({ countryCode: value.trim(), page: 0 });
    });

    // Debounce job title filter input modestly (300ms)
    this.jobTitleControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.updateFilter({ jobTitle: value.trim(), page: 0 });
    });
  }

  private setupDirectoryStream(): void {
    combineLatest([this.filterState$, this.reloadTrigger$]).pipe(
      takeUntilDestroyed(this.destroyRef),
      tap(() => {
        this.loading = true;
        this.directoryError = null;
      }),
      // switchMap cancels prior in-flight requests and avoids stale overwrites
      switchMap(([filters]) =>
        this.employeeApiService.getEmployees(filters).pipe(
          catchError((err: unknown) => {
            const parsed = this.apiErrorService.parseError(err);
            this.directoryError = parsed.message || 'Unable to load employee directory.';
            this.employees = [];
            this.pageInfo = {
              content: [],
              totalElements: 0,
              totalPages: 0,
              page: filters.page ?? 0,
              size: filters.size ?? 20,
              hasNext: false
            };
            this.loading = false;
            return of(null);
          })
        )
      )
    ).subscribe(response => {
      if (response) {
        this.employees = response.content;
        this.pageInfo = response;
        this.directoryError = null;
      }
      this.loading = false;
    });
  }

  private updateFilter(partial: Partial<EmployeeFilterParams>): void {
    const current = this.filterState$.getValue();
    this.filterState$.next({
      ...current,
      ...partial
    });
  }

  resetFilters(): void {
    this.searchControl.setValue('', { emitEvent: false });
    this.countryControl.setValue('', { emitEvent: false });
    this.jobTitleControl.setValue('', { emitEvent: false });
    const current = this.filterState$.getValue();
    this.filterState$.next({
      search: '',
      countryCode: '',
      jobTitle: '',
      page: 0,
      size: current.size ?? 20
    });
  }

  hasActiveFilters(): boolean {
    const current = this.filterState$.getValue();
    return Boolean(
      (current.search && current.search.trim().length > 0) ||
      (current.countryCode && current.countryCode.trim().length > 0) ||
      (current.jobTitle && current.jobTitle.trim().length > 0)
    );
  }

  goToPage(pageIndex: number): void {
    if (pageIndex < 0 || (this.pageInfo.totalPages > 0 && pageIndex >= this.pageInfo.totalPages)) {
      return;
    }
    this.updateFilter({ page: pageIndex });
  }

  onPageSizeChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const newSize = parseInt(target.value, 10);
    if (!isNaN(newSize) && newSize > 0) {
      this.updateFilter({ page: 0, size: newSize });
    }
  }

  reload(): void {
    this.reloadTrigger$.next(this.reloadTrigger$.getValue() + 1);
  }

  // --- Create Employee Workflow ---

  openCreateModal(): void {
    this.createForm.reset({
      employeeNumber: '',
      firstName: '',
      lastName: '',
      email: '',
      countryCode: '',
      jobTitle: '',
      department: '',
      joinedAt: '',
      terminatedAt: ''
    });
    this.createFormError = null;
    this.createFormDetails = [];
    this.savingCreate = false;
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    if (this.savingCreate) {
      return;
    }
    this.showCreateModal = false;
    this.createFormError = null;
    this.createFormDetails = [];
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.savingCreate = true;
    this.createFormError = null;
    this.createFormDetails = [];

    const v = this.createForm.value;
    const request: CreateEmployeeRequest = {
      employeeNumber: v.employeeNumber.trim(),
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      email: v.email.trim(),
      countryCode: v.countryCode.trim(),
      jobTitle: v.jobTitle.trim(),
      department: v.department?.trim() ? v.department.trim() : null,
      joinedAt: v.joinedAt,
      terminatedAt: v.terminatedAt ? v.terminatedAt : null
    };

    this.employeeApiService.createEmployee(request).subscribe({
      next: (created) => {
        this.savingCreate = false;
        this.showCreateModal = false;
        this.setNotification(`Employee ${created.firstName} ${created.lastName} (${created.employeeNumber}) created successfully.`);
        this.reload();
      },
      error: (err: unknown) => {
        this.savingCreate = false;
        const parsed: ApiError = this.apiErrorService.parseError(err);
        this.createFormError = parsed.message || 'Failed to create employee.';
        this.createFormDetails = parsed.details || [];
      }
    });
  }

  // --- Edit Employee Workflow ---

  openEditModal(employee: Employee): void {
    this.selectedEmployee = employee;
    this.editFormError = null;
    this.editFormDetails = [];
    this.savingEdit = false;

    this.editForm.reset({
      firstName: employee.firstName,
      lastName: employee.lastName,
      email: employee.email,
      jobTitle: employee.jobTitle,
      department: employee.department || '',
      terminatedAt: employee.terminatedAt || ''
    });

    this.showEditModal = true;
  }

  closeEditModal(): void {
    if (this.savingEdit) {
      return;
    }
    this.showEditModal = false;
    this.selectedEmployee = null;
    this.editFormError = null;
    this.editFormDetails = [];
  }

  submitEdit(): void {
    if (!this.selectedEmployee) {
      return;
    }
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.savingEdit = true;
    this.editFormError = null;
    this.editFormDetails = [];

    const v = this.editForm.value;
    const request: UpdateEmployeeRequest = {
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      email: v.email.trim(),
      countryCode: this.selectedEmployee.countryCode,
      jobTitle: v.jobTitle.trim(),
      department: v.department?.trim() ? v.department.trim() : null,
      terminatedAt: v.terminatedAt ? v.terminatedAt : null
    };

    this.employeeApiService.updateEmployee(this.selectedEmployee.id, request).subscribe({
      next: (updated) => {
        this.savingEdit = false;
        this.showEditModal = false;
        this.setNotification(`Employee ${updated.firstName} ${updated.lastName} updated successfully.`);
        this.selectedEmployee = null;
        this.reload();
      },
      error: (err: unknown) => {
        this.savingEdit = false;
        const parsed: ApiError = this.apiErrorService.parseError(err);
        this.editFormError = parsed.message || 'Failed to update employee.';
        this.editFormDetails = parsed.details || [];
      }
    });
  }

  // --- Delete Employee Workflow ---

  openDeleteModal(employee: Employee): void {
    this.employeeToDelete = employee;
    this.deleteError = null;
    this.deleting = false;
    this.showDeleteModal = true;
  }

  closeDeleteModal(): void {
    if (this.deleting) {
      return;
    }
    this.showDeleteModal = false;
    this.employeeToDelete = null;
    this.deleteError = null;
  }

  confirmDelete(): void {
    if (!this.employeeToDelete) {
      return;
    }
    const emp = this.employeeToDelete;
    this.deleting = true;
    this.deleteError = null;

    this.employeeApiService.deleteEmployee(emp.id).subscribe({
      next: () => {
        this.deleting = false;
        this.showDeleteModal = false;
        this.employeeToDelete = null;
        this.setNotification(`Employee ${emp.firstName} ${emp.lastName} (${emp.employeeNumber}) was removed.`);

        // Step back one page if last item on a non-first page was removed
        const current = this.filterState$.getValue();
        const currentPage = current.page ?? 0;
        if (this.employees.length <= 1 && currentPage > 0) {
          this.updateFilter({ page: currentPage - 1 });
        } else {
          this.reload();
        }
      },
      error: (err: unknown) => {
        this.deleting = false;
        const parsed: ApiError = this.apiErrorService.parseError(err);
        if (parsed.status === 404) {
          // Graceful handling when employee was already soft-deleted or removed
          this.showDeleteModal = false;
          this.employeeToDelete = null;
          this.setNotification(`Employee ${emp.employeeNumber} is no longer in the active directory.`);
          this.reload();
        } else {
          this.deleteError = parsed.message || 'Failed to delete employee.';
        }
      }
    });
  }

  // --- Helper Methods ---

  setNotification(msg: string): void {
    this.notificationMessage = msg;
    if (this.notificationTimer) {
      clearTimeout(this.notificationTimer);
    }
    this.notificationTimer = setTimeout(() => {
      this.notificationMessage = null;
    }, 5000);
  }

  dismissNotification(): void {
    this.notificationMessage = null;
    if (this.notificationTimer) {
      clearTimeout(this.notificationTimer);
    }
  }

  getCountryName(countryCode: string): string {
    const found = this.supportedCountries.find(c => c.code.toUpperCase() === countryCode.toUpperCase());
    return found ? `${found.name} (${found.code})` : countryCode;
  }

  formatStatus(employee: Employee): string {
    if (employee.terminatedAt) {
      return 'Terminated';
    }
    return 'Active';
  }

  get paginationStart(): number {
    if (this.pageInfo.totalElements === 0) return 0;
    return this.pageInfo.page * this.pageInfo.size + 1;
  }

  get paginationEnd(): number {
    const end = (this.pageInfo.page + 1) * this.pageInfo.size;
    return Math.min(end, this.pageInfo.totalElements);
  }
}
