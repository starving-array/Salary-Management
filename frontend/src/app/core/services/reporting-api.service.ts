import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { catchError, Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ApiErrorService } from './api-error.service';
import {
  CountryJobTitleSalarySummary,
  CountrySalarySummary
} from '../../models/reporting.model';

@Injectable({
  providedIn: 'root'
})
export class ReportingApiService {

  constructor(
    private readonly http: HttpClient,
    private readonly errorService: ApiErrorService,
    @Inject(API_BASE_URL) private readonly baseUrl: string
  ) {}

  getCountrySalarySummary(
    countryCode: string,
    asOfDate?: string,
    jobTitle?: string
  ): Observable<CountrySalarySummary> {
    let params = new HttpParams();
    if (asOfDate) {
      params = params.set('asOfDate', asOfDate);
    }
    if (jobTitle && jobTitle.trim().length > 0) {
      params = params.set('jobTitle', jobTitle.trim());
    }

    return this.http.get<CountrySalarySummary>(
      `${this.baseUrl}/reports/countries/${encodeURIComponent(countryCode)}/salary-summary`,
      { params }
    ).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }

  getSalaryByJobTitle(
    countryCode: string,
    asOfDate?: string,
    jobTitle?: string
  ): Observable<CountryJobTitleSalarySummary> {
    let params = new HttpParams();
    if (asOfDate) {
      params = params.set('asOfDate', asOfDate);
    }
    if (jobTitle && jobTitle.trim().length > 0) {
      params = params.set('jobTitle', jobTitle.trim());
    }

    return this.http.get<CountryJobTitleSalarySummary>(
      `${this.baseUrl}/reports/countries/${encodeURIComponent(countryCode)}/salary-by-job-title`,
      { params }
    ).pipe(
      catchError(err => this.errorService.handleError(err))
    );
  }
}
