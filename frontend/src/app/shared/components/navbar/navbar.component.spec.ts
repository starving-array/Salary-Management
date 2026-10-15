import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NavbarComponent } from './navbar.component';

describe('NavbarComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should create navbar', () => {
    const fixture = TestBed.createComponent(NavbarComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should toggle mobile navigation state', () => {
    const fixture = TestBed.createComponent(NavbarComponent);
    const component = fixture.componentInstance;
    expect(component.navOpen).toBe(false);
    component.toggleNav();
    expect(component.navOpen).toBe(true);
    component.toggleNav();
    expect(component.navOpen).toBe(false);
  });

  it('should display brand name and portal badge', () => {
    const fixture = TestBed.createComponent(NavbarComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand-title')?.textContent).toContain('Salary Management');
    expect(compiled.querySelector('.brand-badge')?.textContent).toContain('HR Portal');
  });
});
