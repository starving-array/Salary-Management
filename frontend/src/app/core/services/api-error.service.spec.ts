import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ApiErrorService } from './api-error.service';

describe('ApiErrorService', () => {
  let service: ApiErrorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ApiErrorService);
  });

  it('should parse structured backend ErrorResponse', () => {
    const errorResponse = new HttpErrorResponse({
      status: 400,
      statusText: 'Bad Request',
      error: {
        timestamp: '2026-10-15T10:00:00Z',
        status: 400,
        error: 'Bad Request',
        message: 'Validation failed',
        details: ['email: must be a well-formed email address', 'firstName: must not be blank']
      }
    });

    const parsed = service.parseError(errorResponse);
    expect(parsed.status).toBe(400);
    expect(parsed.message).toBe('Validation failed');
    expect(parsed.details).toEqual([
      'email: must be a well-formed email address',
      'firstName: must not be blank'
    ]);
  });

  it('should format message with validation details', () => {
    const errorResponse = new HttpErrorResponse({
      status: 400,
      error: {
        timestamp: '2026-10-15T10:00:00Z',
        status: 400,
        error: 'Bad Request',
        message: 'Validation failed',
        details: ['field1: invalid', 'field2: required']
      }
    });

    const formatted = service.formatErrorMessage(errorResponse);
    expect(formatted).toBe('Validation failed: field1: invalid, field2: required');
  });

  it('should handle network connection failure status 0', () => {
    const networkError = new HttpErrorResponse({
      status: 0,
      statusText: 'Unknown Error'
    });

    const parsed = service.parseError(networkError);
    expect(parsed.status).toBe(0);
    expect(parsed.error).toBe('Network Error');
    expect(parsed.message).toContain('Unable to connect to the salary management server');
  });

  it('should handle non-HTTP errors gracefully', () => {
    const standardError = new Error('Client unexpected error');
    const parsed = service.parseError(standardError);
    expect(parsed.status).toBe(500);
    expect(parsed.message).toBe('Client unexpected error');
  });
});
