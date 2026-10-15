import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, EventEmitter, HostListener, inject, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
export class CompensationModalComponent implements OnInit {
  @Input({ required: true }) employee!: Employee;
  @Output() close = new EventEmitter<void>();
  @Output() compensationUpdated = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly compensationApiService = inject(CompensationApiService);
  private readonly apiErrorService = inject(ApiErrorService);

  records: CompensationRecord[] = [];
  loading = false;
  historyError: string | null = null;
  notificationMessage: string | null = null;

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

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (!this.savingInitial && !this.savingChange) {
      this.closeModal();
    }
  }

  closeModal(): void {
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
    this.loading = true;
    this.historyError = null;

    this.compensationApiService.getCompensationHistory(this.employee.id).subscribe({
      next: (history) => {
        // Backend returns records ordered by effectiveFrom ASC, createdAt ASC
        this.records = history;
        this.loading = false;
      },
      error: (err: unknown) => {
        this.loading = false;
        const parsed: ApiError = this.apiErrorService.parseError(err);
        if (parsed.status === 404) {
          this.historyError = 'Employee not found or has been removed from active records.';
        } else {
          this.historyError = parsed.message || 'Failed to load compensation history.';
        }
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

    const raw = this.initialForm.value;
    const request: CreateCompensationRequest = {
      annualSalary: Number(raw.annualSalary),
      effectiveFrom: raw.effectiveFrom
    };

    this.compensationApiService.assignInitialSalary(this.employee.id, request).subscribe({
      next: () => {
        this.savingInitial = false;
        this.setNotification('Initial compensation assigned successfully.');
        this.initialForm.reset({ effectiveFrom: this.employee.joinedAt });
        this.compensationUpdated.emit();
        this.loadHistory();
      },
      error: (err: unknown) => {
        this.savingInitial = false;
        const parsed: ApiError = this.apiErrorService.parseError(err);
        this.initialError = parsed.message || 'Failed to assign initial salary.';
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

    const raw = this.changeForm.value;
    const request: ChangeSalaryRequest = {
      annualSalary: Number(raw.annualSalary),
      effectiveFrom: raw.effectiveFrom
    };

    this.compensationApiService.recordSalaryChange(this.employee.id, request).subscribe({
      next: () => {
        this.savingChange = false;
        this.setNotification('Salary change recorded successfully.');
        this.changeForm.reset();
        this.compensationUpdated.emit();
        this.loadHistory();
      },
      error: (err: unknown) => {
        this.savingChange = false;
        const parsed: ApiError = this.apiErrorService.parseError(err);
        this.changeError = parsed.message || 'Failed to record salary change.';
      }
    });
  }

  setNotification(msg: string): void {
    this.notificationMessage = msg;
    setTimeout(() => {
      if (this.notificationMessage === msg) {
        this.notificationMessage = null;
      }
    }, 5000);
  }

  dismissNotification(): void {
    this.notificationMessage = null;
  }
}
