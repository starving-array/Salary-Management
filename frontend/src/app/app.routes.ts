import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { EmployeesComponent } from './pages/employees/employees.component';
import { PlaceholderComponent } from './pages/placeholder/placeholder.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    title: 'Dashboard - Salary Management'
  },
  {
    path: 'employees',
    component: EmployeesComponent,
    title: 'Employees - Salary Management'
  },
  {
    path: 'reports',
    component: PlaceholderComponent,
    title: 'Salary Reports - Salary Management',
    data: {
      moduleName: 'Salary Analytics & Country Reports',
      phase: 'Phase 5C',
      description: 'Country salary statistics (min, max, average) and job-title breakdowns with historical date selection will be implemented in Phase 5C.'
    }
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
