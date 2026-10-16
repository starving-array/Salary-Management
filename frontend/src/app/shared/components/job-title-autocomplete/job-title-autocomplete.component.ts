import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { EmployeeApiService } from '../../../core/services/employee-api.service';

@Component({
  selector: 'app-job-title-autocomplete',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-title-autocomplete.component.html',
  styleUrl: './job-title-autocomplete.component.css'
})
export class JobTitleAutocompleteComponent implements OnInit, OnDestroy {
  private readonly employeeApiService = inject(EmployeeApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) control!: FormControl<string>;
  @Input() inputId = 'job-title-filter';
  @Input() placeholder = 'Filter by job title...';
  @Input() ariaLabel = 'Filter by job title';
  @Input() listboxId = 'job-title-listbox';
  @Input() optionIdPrefix = 'job-title-option-';

  @Output() selected = new EventEmitter<string>();

  jobTitleSuggestions: string[] = [];
  showJobTitleSuggestions = false;
  loadingJobTitleSuggestions = false;
  activeSuggestionIndex = -1;
  private jobTitleBlurTimer: ReturnType<typeof setTimeout> | null = null;

  get activeSuggestionId(): string | null {
    if (this.showJobTitleSuggestions && this.activeSuggestionIndex >= 0) {
      return `${this.optionIdPrefix}${this.activeSuggestionIndex}`;
    }
    return null;
  }

  ngOnInit(): void {
    if (!this.control) {
      this.control = new FormControl('', { nonNullable: true });
    }
    this.setupAutocompleteStream();
  }

  ngOnDestroy(): void {
    if (this.jobTitleBlurTimer) {
      clearTimeout(this.jobTitleBlurTimer);
    }
  }

  private setupAutocompleteStream(): void {
    this.control.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      const trimmed = (value ?? '').trim();

      if (trimmed.length >= 2) {
        this.loadingJobTitleSuggestions = true;
        this.showJobTitleSuggestions = true;
        this.cdr.markForCheck();

        this.employeeApiService.getJobTitles(trimmed, 10).subscribe({
          next: (titles) => {
            if ((this.control.value ?? '').trim() === trimmed) {
              this.jobTitleSuggestions = titles;
              this.loadingJobTitleSuggestions = false;
              this.activeSuggestionIndex = -1;
              this.cdr.markForCheck();
            }
          },
          error: () => {
            if ((this.control.value ?? '').trim() === trimmed) {
              this.jobTitleSuggestions = [];
              this.loadingJobTitleSuggestions = false;
              this.activeSuggestionIndex = -1;
              this.cdr.markForCheck();
            }
          }
        });
      } else {
        this.closeJobTitleSuggestions();
      }
    });
  }

  selectJobTitleSuggestion(suggestion: string): void {
    this.control.setValue(suggestion, { emitEvent: false });
    this.closeJobTitleSuggestions();
    this.selected.emit(suggestion);
    this.cdr.markForCheck();
  }

  closeJobTitleSuggestions(): void {
    this.showJobTitleSuggestions = false;
    this.jobTitleSuggestions = [];
    this.activeSuggestionIndex = -1;
    this.loadingJobTitleSuggestions = false;
    this.cdr.markForCheck();
  }

  onJobTitleKeyDown(event: KeyboardEvent): void {
    if (!this.showJobTitleSuggestions) {
      if (event.key === 'ArrowDown' && (this.control.value ?? '').trim().length >= 2) {
        this.showJobTitleSuggestions = true;
        this.cdr.markForCheck();
      }
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (this.jobTitleSuggestions.length > 0) {
          this.activeSuggestionIndex = (this.activeSuggestionIndex + 1) % this.jobTitleSuggestions.length;
          this.cdr.markForCheck();
        }
        break;

      case 'ArrowUp':
        event.preventDefault();
        if (this.jobTitleSuggestions.length > 0) {
          this.activeSuggestionIndex = this.activeSuggestionIndex <= 0
            ? this.jobTitleSuggestions.length - 1
            : this.activeSuggestionIndex - 1;
          this.cdr.markForCheck();
        }
        break;

      case 'Enter':
        if (this.activeSuggestionIndex >= 0 && this.activeSuggestionIndex < this.jobTitleSuggestions.length) {
          event.preventDefault();
          this.selectJobTitleSuggestion(this.jobTitleSuggestions[this.activeSuggestionIndex]);
        }
        break;

      case 'Escape':
        event.preventDefault();
        event.stopPropagation();
        this.closeJobTitleSuggestions();
        break;
    }
  }

  onJobTitleFocus(): void {
    if ((this.control.value ?? '').trim().length >= 2 && this.jobTitleSuggestions.length > 0) {
      this.showJobTitleSuggestions = true;
      this.cdr.markForCheck();
    }
  }

  onJobTitleBlur(): void {
    if (this.jobTitleBlurTimer) {
      clearTimeout(this.jobTitleBlurTimer);
    }
    this.jobTitleBlurTimer = setTimeout(() => {
      this.showJobTitleSuggestions = false;
      this.activeSuggestionIndex = -1;
      this.cdr.markForCheck();
    }, 200);
  }

  @HostListener('document:keydown.escape')
  onDocumentEscape(): void {
    if (this.showJobTitleSuggestions) {
      this.closeJobTitleSuggestions();
    }
  }
}
