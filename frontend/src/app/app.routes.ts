import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
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
    component: PlaceholderComponent,
    title: 'Employees - Salary Management',
    data: {
      moduleName: 'Employee Directory & Compensation',
      phase: 'Phase 5B',
      description: 'Employee directory search, country & job title filters, pagination, detail editing, and compensation history workflows will be implemented in Phase 5B.'
    }
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
