import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { catchError, Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ApiErrorService } from './api-error.service';
import {
  CreateEmployeeRequest,
  Employee,
  EmployeeFilterParams,
  EmployeePageResponse,
  UpdateEmployeeRequest
} from '../../models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class EmployeeApiService {

  constructor(
    private readonly http: HttpClient,
    private readonly errorService: ApiErrorService,
    @Inject(API_BASE_URL) private readonly baseUrl: string
  ) {}

  getEmployees(filter?: EmployeeFilterParams): Observable<EmployeePageResponse> {
    let params = new HttpParams();
    if (filter) {
      if (filter.search !== undefined && filter.search !== null && filter.search.trim().length > 0) {
        params = params.set('search', filter.search.trim());
      }
      if (filter.countryCode !== undefined && filter.countryCode !== null && filter.countryCode.trim().length > 0) {
        params = params.set('countryCode', filter.countryCode.trim());
      }
      if (filter.jobTitle !== undefined && filter.jobTitle !== null && filter.jobTitle.trim().length > 0) {
        params = params.set('jobTitle', filter.jobTitle.trim());
      }
      if (filter.page !== undefined && filter.page !== null) {
        params = params.set('page', filter.page.toString());
      }
      if (filter.size !== undefined && filter.size !== null) {
        params = params.set('size', filter.size.toString());
      }
    }

    return this.http.get<EmployeePageResponse>(`${this.baseUrl}/employees`, { params }).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }

  getEmployee(id: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/employees/${id}`).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }

  createEmployee(request: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(`${this.baseUrl}/employees`, request).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }

  updateEmployee(id: string, request: UpdateEmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/employees/${id}`, request).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }

  deleteEmployee(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/employees/${id}`).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }
}
