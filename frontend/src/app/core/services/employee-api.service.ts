import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { catchError, Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ApiErrorService } from './api-error.service';
import { CreateEmployeeRequest, Employee, UpdateEmployeeRequest } from '../../models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class EmployeeApiService {

  constructor(
    private readonly http: HttpClient,
    private readonly errorService: ApiErrorService,
    @Inject(API_BASE_URL) private readonly baseUrl: string
  ) {}

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
