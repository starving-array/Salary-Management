import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { ReportingApiService } from '../../core/services/reporting-api.service';
import { ApiError } from '../../models/api-error.model';
import { SUPPORTED_COUNTRIES } from '../../models/country.model';
import {
  CountryJobTitleSalarySummary,
  CountrySalarySummary
} from '../../models/reporting.model';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, CurrencyPipe],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  private readonly reportingApiService = inject(ReportingApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly supportedCountries = SUPPORTED_COUNTRIES;

  // Filter controls
  readonly countryControl = new FormControl('US', { nonNullable: true, validators: [Validators.required] });
  readonly asOfDateControl = new FormControl(this.getTodayDateString(), { nonNullable: true });
  readonly jobTitleFilterControl = new FormControl('', { nonNullable: true });

  // Reporting state
  summary: CountrySalarySummary | null = null;
  jobTitleSummary: CountryJobTitleSalarySummary | null = null;

  loadingSummary = false;
  loadingJobTitles = false;

  summaryError: string | null = null;
  jobTitlesError: string | null = null;

  ngOnInit(): void {
    // Initial report load
    this.loadReports();

    // Re-fetch both reports when country changes
    this.countryControl.valueChanges
      .pipe(
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.loadReports();
      });

    // Re-fetch both reports when asOfDate changes
    this.asOfDateControl.valueChanges
      .pipe(
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.loadReports();
      });

    // Re-fetch job titles breakdown when job title filter changes (debounced)
    this.jobTitleFilterControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.loadJobTitlesReport();
      });
  }

  loadReports(): void {
    this.loadSummaryReport();
    this.loadJobTitlesReport();
  }

  loadSummaryReport(): void {
    const countryCode = this.countryControl.value?.trim();
    if (!countryCode) {
      return;
    }
    const asOfDate = this.asOfDateControl.value?.trim() || undefined;

    this.loadingSummary = true;
    this.summaryError = null;

    this.reportingApiService.getCountrySalarySummary(countryCode, asOfDate)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.summary = data;
          this.loadingSummary = false;
        },
        error: (err: ApiError) => {
          this.loadingSummary = false;
          this.summaryError = err?.message || 'Failed to load country salary summary.';
        }
      });
  }

  loadJobTitlesReport(): void {
    const countryCode = this.countryControl.value?.trim();
    if (!countryCode) {
      return;
    }
    const asOfDate = this.asOfDateControl.value?.trim() || undefined;
    const jobTitle = this.jobTitleFilterControl.value?.trim() || undefined;

    this.loadingJobTitles = true;
    this.jobTitlesError = null;

    this.reportingApiService.getSalaryByJobTitle(countryCode, asOfDate, jobTitle)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.jobTitleSummary = data;
          this.loadingJobTitles = false;
        },
        error: (err: ApiError) => {
          this.loadingJobTitles = false;
          this.jobTitlesError = err?.message || 'Failed to load job title salary report.';
        }
      });
  }

  resetFilters(): void {
    this.countryControl.setValue('US', { emitEvent: false });
    this.asOfDateControl.setValue(this.getTodayDateString(), { emitEvent: false });
    this.jobTitleFilterControl.setValue('', { emitEvent: false });
    this.loadReports();
  }

  clearJobTitleFilter(): void {
    this.jobTitleFilterControl.setValue('');
  }

  private getTodayDateString(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
