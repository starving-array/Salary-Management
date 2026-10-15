import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { catchError, Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ApiErrorService } from './api-error.service';
import {
  ChangeSalaryRequest,
  CompensationRecord,
  CreateCompensationRequest
} from '../../models/compensation.model';

@Injectable({
  providedIn: 'root'
})
export class CompensationApiService {

  constructor(
    private readonly http: HttpClient,
    private readonly errorService: ApiErrorService,
    @Inject(API_BASE_URL) private readonly baseUrl: string
  ) {}

  assignInitialSalary(employeeId: string, request: CreateCompensationRequest): Observable<CompensationRecord> {
    return this.http.post<CompensationRecord>(
      `${this.baseUrl}/employees/${employeeId}/compensations`,
      request
    ).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }

  getCompensationHistory(employeeId: string): Observable<CompensationRecord[]> {
    return this.http.get<CompensationRecord[]>(
      `${this.baseUrl}/employees/${employeeId}/compensations`
    ).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }

  recordSalaryChange(employeeId: string, request: ChangeSalaryRequest): Observable<CompensationRecord> {
    return this.http.post<CompensationRecord>(
      `${this.baseUrl}/employees/${employeeId}/compensations/changes`,
      request
    ).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }
}
