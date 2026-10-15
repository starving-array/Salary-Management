import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { API_BASE_URL } from '../../core/config/api.config';

describe('DashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: API_BASE_URL, useValue: '/api/v1' }
      ]
    }).compileComponents();
  });

  it('should create dashboard', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should display configured API base URL', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.detail-value')?.textContent).toContain('/api/v1');
  });

  it('should render modules cards for Employee, Compensation, and Reporting', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const titles = Array.from(compiled.querySelectorAll('.card-title')).map(el => el.textContent?.trim());
    expect(titles).toContain('Employee Directory');
    expect(titles).toContain('Compensation History');
    expect(titles).toContain('Salary & Country Analytics');
  });
});
