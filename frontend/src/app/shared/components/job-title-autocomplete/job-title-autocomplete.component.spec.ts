import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { of } from 'rxjs';
import { EmployeeApiService } from '../../../core/services/employee-api.service';
import { JobTitleAutocompleteComponent } from './job-title-autocomplete.component';

describe('JobTitleAutocompleteComponent', () => {
  let component: JobTitleAutocompleteComponent;
  let fixture: ComponentFixture<JobTitleAutocompleteComponent>;
  let employeeApiService: {
    getJobTitles: ReturnType<typeof vi.fn>;
  };
  let testControl: FormControl<string>;

  beforeEach(async () => {
    employeeApiService = {
      getJobTitles: vi.fn().mockReturnValue(of(['Software Engineer', 'Senior Software Engineer']))
    };

    await TestBed.configureTestingModule({
      imports: [JobTitleAutocompleteComponent],
      providers: [
        { provide: EmployeeApiService, useValue: employeeApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JobTitleAutocompleteComponent);
    component = fixture.componentInstance;
    testControl = new FormControl('', { nonNullable: true });
    component.control = testControl;
  });

  it('should create autocomplete component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should fetch and display suggestions after 300ms debounce when input length >= 2', () => {
    vi.useFakeTimers();
    fixture.detectChanges();

    testControl.setValue('soft');
    vi.advanceTimersByTime(300);
    fixture.detectChanges();

    expect(employeeApiService.getJobTitles).toHaveBeenCalledWith('soft', 10);
    expect(component.showJobTitleSuggestions).toBe(true);
    expect(component.jobTitleSuggestions.length).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    const items = compiled.querySelectorAll('.autocomplete-item');
    expect(items.length).toBe(2);
    expect(items[0].textContent?.trim()).toBe('Software Engineer');
    expect(items[1].textContent?.trim()).toBe('Senior Software Engineer');
    vi.useRealTimers();
  });

  it('should not fetch suggestions when input length < 2', () => {
    vi.useFakeTimers();
    fixture.detectChanges();

    testControl.setValue('s');
    vi.advanceTimersByTime(350);
    fixture.detectChanges();

    expect(employeeApiService.getJobTitles).not.toHaveBeenCalled();
    expect(component.showJobTitleSuggestions).toBe(false);
    vi.useRealTimers();
  });

  it('should display empty message when no suggestions match', () => {
    vi.useFakeTimers();
    employeeApiService.getJobTitles.mockReturnValue(of([]));
    fixture.detectChanges();

    testControl.setValue('unknownrole');
    vi.advanceTimersByTime(300);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const emptyItem = compiled.querySelector('.autocomplete-empty');
    expect(emptyItem).toBeTruthy();
    expect(emptyItem?.textContent?.trim()).toBe('No matching job titles');
    vi.useRealTimers();
  });

  it('should support keyboard navigation: ArrowDown, ArrowUp, Enter, and Escape', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    const mockTitles = ['Software Engineer', 'Senior Software Engineer'];
    employeeApiService.getJobTitles.mockReturnValue(of(mockTitles));

    testControl.setValue('soft');
    vi.advanceTimersByTime(300);
    fixture.detectChanges();

    expect(component.showJobTitleSuggestions).toBe(true);
    expect(component.activeSuggestionIndex).toBe(-1);

    // ArrowDown -> first item
    component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    expect(component.activeSuggestionIndex).toBe(0);
    expect(component.activeSuggestionId).toBe('job-title-option-0');

    // ArrowDown -> second item
    component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    expect(component.activeSuggestionIndex).toBe(1);
    expect(component.activeSuggestionId).toBe('job-title-option-1');

    // ArrowDown wraps to index 0
    component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    expect(component.activeSuggestionIndex).toBe(0);

    // ArrowUp wraps back to index 1
    component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
    expect(component.activeSuggestionIndex).toBe(1);

    // Enter selects active suggestion and emits event
    let selectedTitle: string | null = null;
    component.selected.subscribe(title => selectedTitle = title);
    component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(testControl.value).toBe('Senior Software Engineer');
    expect(selectedTitle).toBe('Senior Software Engineer');
    expect(component.showJobTitleSuggestions).toBe(false);

    // Escape closes suggestions
    component.jobTitleSuggestions = mockTitles;
    component.showJobTitleSuggestions = true;
    component.onJobTitleKeyDown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(component.showJobTitleSuggestions).toBe(false);

    vi.useRealTimers();
  });

  it('should select suggestion by click and emit selected event', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    const mockTitles = ['DevOps Engineer', 'Software Engineer'];
    employeeApiService.getJobTitles.mockReturnValue(of(mockTitles));

    testControl.setValue('eng');
    vi.advanceTimersByTime(300);
    fixture.detectChanges();

    let selectedTitle: string | null = null;
    component.selected.subscribe(title => selectedTitle = title);

    component.selectJobTitleSuggestion('DevOps Engineer');

    expect(testControl.value).toBe('DevOps Engineer');
    expect(selectedTitle).toBe('DevOps Engineer');
    expect(component.showJobTitleSuggestions).toBe(false);

    // Verify no redundant autocomplete request after selection
    employeeApiService.getJobTitles.mockClear();
    vi.advanceTimersByTime(350);
    expect(employeeApiService.getJobTitles).not.toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('should hide suggestions on blur after timeout', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    component.jobTitleSuggestions = ['Software Engineer'];
    component.showJobTitleSuggestions = true;

    component.onJobTitleBlur();
    expect(component.showJobTitleSuggestions).toBe(true);

    vi.advanceTimersByTime(250);
    expect(component.showJobTitleSuggestions).toBe(false);
    vi.useRealTimers();
  });

  it('should re-open suggestions on focus if query >= 2 chars and suggestions exist', () => {
    fixture.detectChanges();
    testControl.setValue('Software Engineer');
    component.jobTitleSuggestions = ['Software Engineer'];
    component.showJobTitleSuggestions = false;

    component.onJobTitleFocus();
    expect(component.showJobTitleSuggestions).toBe(true);
  });
});
