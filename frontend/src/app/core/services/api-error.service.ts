import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { ApiError } from '../../models/api-error.model';

@Injectable({
  providedIn: 'root'
})
export class ApiErrorService {

  parseError(error: unknown): ApiError {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return {
          timestamp: new Date().toISOString(),
          status: 0,
          error: 'Network Error',
          message: 'Unable to connect to the salary management server. Please check your backend connection.',
          details: []
        };
      }

      const backendError = error.error;
      if (backendError && typeof backendError === 'object' && 'message' in backendError) {
        return {
          timestamp: backendError.timestamp || new Date().toISOString(),
          status: backendError.status || error.status,
          error: backendError.error || error.statusText,
          message: backendError.message,
          details: Array.isArray(backendError.details) ? backendError.details : []
        };
      }

      return {
        timestamp: new Date().toISOString(),
        status: error.status,
        error: error.statusText,
        message: error.message || 'An unexpected server error occurred.',
        details: []
      };
    }

    return {
      timestamp: new Date().toISOString(),
      status: 500,
      error: 'Client Error',
      message: error instanceof Error ? error.message : 'An unknown error occurred.',
      details: []
    };
  }

  formatErrorMessage(error: unknown): string {
    const apiError = this.parseError(error);
    if (apiError.details && apiError.details.length > 0) {
      return `${apiError.message}: ${apiError.details.join(', ')}`;
    }
    return apiError.message;
  }

  handleError(error: unknown): Observable<never> {
    const parsed = this.parseError(error);
    return throwError(() => parsed);
  }
}
