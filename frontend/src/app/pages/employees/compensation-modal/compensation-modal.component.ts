import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, Subscription } from 'rxjs';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { CompensationApiService } from '../../../core/services/compensation-api.service';
import { ApiError } from '../../../models/api-error.model';
import {
  ChangeSalaryRequest,
  CompensationRecord,
  CreateCompensationRequest
} from '../../../models/compensation.model';
import { SUPPORTED_COUNTRIES } from '../../../models/country.model';
import { Employee } from '../../../models/employee.model';

@Component({
  selector: 'app-compensation-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, CurrencyPipe],
  templateUrl: './compensation-modal.component.html',
  styleUrl: './compensation-modal.component.css'
})
export class CompensationModalComponent implements OnInit, OnChanges, OnDestroy {
  @Input({ required: true }) employee!: Employee;
  @Output() close = new EventEmitter<void>();
  @Output() compensationUpdated = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly compensationApiService = inject(CompensationApiService);
  private readonly apiErrorService = inject(ApiErrorService);
  private readonly cdr = inject(ChangeDetectorRef);

  records: CompensationRecord[] = [];
  loading = false;
  historyError: string | null = null;
  notificationMessage: string | null = null;
  private notificationTimer: ReturnType<typeof setTimeout> | null = null;
  private historySubscription: Subscription | null = null;

  // Initial Salary Form State
  savingInitial = false;
  initialError: string | null = null;
  initialForm: FormGroup = this.fb.group({
    annualSalary: ['', [Validators.required, Validators.min(0.01)]],
    effectiveFrom: ['', [Validators.required]]
  });

  // Salary Change Form State
  savingChange = false;
  changeError: string | null = null;
  changeForm: FormGroup = this.fb.group({
    annualSalary: ['', [Validators.required, Validators.min(0.01)]],
    effectiveFrom: ['', [Validators.required]]
  });

  ngOnInit(): void {
    if (this.employee) {
      // Default initial salary effective date to employee joining date
      this.initialForm.patchValue({
        effectiveFrom: this.employee.joinedAt
      });
      this.loadHistory();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['employee'] && !changes['employee'].firstChange && this.employee) {
      this.initialForm.patchValue({
        effectiveFrom: this.employee.joinedAt
      });
      this.records = [];
      this.historyError = null;
      this.notificationMessage = null;
      this.loadHistory();
    }
  }

  ngOnDestroy(): void {
    if (this.historySubscription) {
      this.historySubscription.unsubscribe();
      this.historySubscription = null;
    }
    if (this.notificationTimer) {
      clearTimeout(this.notificationTimer);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (!this.savingInitial && !this.savingChange) {
      this.closeModal();
    }
  }

  closeModal(): void {
    if (this.historySubscription) {
      this.historySubscription.unsubscribe();
      this.historySubscription = null;
    }
    this.close.emit();
  }

  get currencyCode(): string {
    if (this.records.length > 0 && this.records[0].currencyCode) {
      return this.records[0].currencyCode;
    }
    const found = SUPPORTED_COUNTRIES.find(
      c => c.code.toUpperCase() === this.employee.countryCode.toUpperCase()
    );
    return found ? found.currencyCode : 'USD';
  }

  get countryDisplayName(): string {
    const found = SUPPORTED_COUNTRIES.find(
      c => c.code.toUpperCase() === this.employee.countryCode.toUpperCase()
    );
    return found ? `${found.name} (${found.code})` : this.employee.countryCode;
  }

  get hasHistory(): boolean {
    return this.records.length > 0;
  }

  loadHistory(): void {
    if (!this.employee) return;

    if (this.historySubscription) {
      this.historySubscription.unsubscribe();
      this.historySubscription = null;
    }

    this.loading = true;
    this.historyError = null;
    this.cdr.markForCheck();

    this.historySubscription = this.compensationApiService.getCompensationHistory(this.employee.id)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (history) => {
          // Backend returns records ordered by effectiveFrom ASC, createdAt ASC
          this.records = history;
          this.historyError = null;
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          const parsed: ApiError = this.apiErrorService.parseError(err);
          if (parsed.status === 404) {
            this.historyError = 'Employee not found or has been removed from active records.';
          } else {
            this.historyError = parsed.message || 'Failed to load compensation history.';
          }
          this.records = [];
          this.cdr.markForCheck();
        }
      });
  }

  submitInitialSalary(): void {
    if (this.initialForm.invalid) {
      this.initialForm.markAllAsTouched();
      return;
    }

    this.savingInitial = true;
    this.initialError = null;
    this.cdr.markForCheck();

    const raw = this.initialForm.value;
    const request: CreateCompensationRequest = {
      annualSalary: Number(raw.annualSalary),
      effectiveFrom: raw.effectiveFrom
    };

    this.compensationApiService.assignInitialSalary(this.employee.id, request)
      .pipe(
        finalize(() => {
          this.savingInitial = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.setNotification('Initial compensation assigned successfully.');
          this.initialForm.reset({ effectiveFrom: this.employee.joinedAt });
          this.compensationUpdated.emit();
          this.loadHistory();
        },
        error: (err: unknown) => {
          const parsed: ApiError = this.apiErrorService.parseError(err);
          this.initialError = parsed.message || 'Failed to assign initial salary.';
          this.cdr.markForCheck();
        }
      });
  }

  submitSalaryChange(): void {
    if (this.changeForm.invalid) {
      this.changeForm.markAllAsTouched();
      return;
    }

    this.savingChange = true;
    this.changeError = null;
    this.cdr.markForCheck();

    const raw = this.changeForm.value;
    const request: ChangeSalaryRequest = {
      annualSalary: Number(raw.annualSalary),
      effectiveFrom: raw.effectiveFrom
    };

    this.compensationApiService.recordSalaryChange(this.employee.id, request)
      .pipe(
        finalize(() => {
          this.savingChange = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.setNotification('Salary change recorded successfully.');
          this.changeForm.reset();
          this.compensationUpdated.emit();
          this.loadHistory();
        },
        error: (err: unknown) => {
          const parsed: ApiError = this.apiErrorService.parseError(err);
          this.changeError = parsed.message || 'Failed to record salary change.';
          this.cdr.markForCheck();
        }
      });
  }

  setNotification(msg: string): void {
    this.notificationMessage = msg;
    if (this.notificationTimer) {
      clearTimeout(this.notificationTimer);
    }
    this.notificationTimer = setTimeout(() => {
      this.notificationMessage = null;
      this.cdr.markForCheck();
    }, 5000);
    this.cdr.markForCheck();
  }

  dismissNotification(): void {
    this.notificationMessage = null;
    if (this.notificationTimer) {
      clearTimeout(this.notificationTimer);
    }
    this.cdr.markForCheck();
  }
}
