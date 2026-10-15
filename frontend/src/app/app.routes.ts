import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { EmployeesComponent } from './pages/employees/employees.component';
import { ReportsComponent } from './pages/reports/reports.component';

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
    component: ReportsComponent,
    title: 'Salary Reports - Salary Management'
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
