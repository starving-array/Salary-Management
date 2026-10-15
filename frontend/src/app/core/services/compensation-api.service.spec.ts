import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { CompensationApiService } from './compensation-api.service';
import {
  ChangeSalaryRequest,
  CompensationRecord,
  CreateCompensationRequest
} from '../../models/compensation.model';

describe('CompensationApiService', () => {
  let service: CompensationApiService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/v1';
  const employeeId = '123e4567-e89b-12d3-a456-426614174000';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CompensationApiService,
        { provide: API_BASE_URL, useValue: baseUrl }
      ]
    });

    service = TestBed.inject(CompensationApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should assign initial salary', () => {
    const request: CreateCompensationRequest = {
      annualSalary: 85000.00,
      effectiveFrom: '2025-01-01'
    };

    const mockResponse: CompensationRecord = {
      id: 'comp-uuid-001',
      employeeId,
      annualSalary: 85000.00,
      currencyCode: 'USD',
      effectiveFrom: '2025-01-01',
      effectiveTo: null,
      createdAt: '2025-01-01T00:00:00Z'
    };

    service.assignInitialSalary(employeeId, request).subscribe(record => {
      expect(record.annualSalary).toBe(85000.00);
      expect(record.currencyCode).toBe('USD');
    });

    const req = httpMock.expectOne(`${baseUrl}/employees/${employeeId}/compensations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse, { status: 201, statusText: 'Created' });
  });

  it('should fetch compensation history in order', () => {
    const mockHistory: CompensationRecord[] = [
      {
        id: 'c1',
        employeeId,
        annualSalary: 60000.00,
        currencyCode: 'USD',
        effectiveFrom: '2024-01-01',
        effectiveTo: '2025-01-01',
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 'c2',
        employeeId,
        annualSalary: 75000.00,
        currencyCode: 'USD',
        effectiveFrom: '2025-01-01',
        effectiveTo: null,
        createdAt: '2025-01-01T00:00:00Z'
      }
    ];

    service.getCompensationHistory(employeeId).subscribe(history => {
      expect(history.length).toBe(2);
      expect(history[0].annualSalary).toBe(60000.00);
      expect(history[1].annualSalary).toBe(75000.00);
    });

    const req = httpMock.expectOne(`${baseUrl}/employees/${employeeId}/compensations`);
    expect(req.request.method).toBe('GET');
    req.flush(mockHistory);
  });

  it('should record salary change effective on date', () => {
    const request: ChangeSalaryRequest = {
      annualSalary: 95000.00,
      effectiveFrom: '2025-07-01'
    };

    const mockResponse: CompensationRecord = {
      id: 'c3',
      employeeId,
      annualSalary: 95000.00,
      currencyCode: 'USD',
      effectiveFrom: '2025-07-01',
      effectiveTo: null,
      createdAt: '2025-07-01T00:00:00Z'
    };

    service.recordSalaryChange(employeeId, request).subscribe(record => {
      expect(record.annualSalary).toBe(95000.00);
      expect(record.effectiveFrom).toBe('2025-07-01');
    });

    const req = httpMock.expectOne(`${baseUrl}/employees/${employeeId}/compensations/changes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse, { status: 201, statusText: 'Created' });
  });
});
