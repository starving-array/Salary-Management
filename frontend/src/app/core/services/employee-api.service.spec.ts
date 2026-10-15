import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { EmployeeApiService } from './employee-api.service';
import { CreateEmployeeRequest, Employee, UpdateEmployeeRequest } from '../../models/employee.model';

describe('EmployeeApiService', () => {
  let service: EmployeeApiService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/v1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        EmployeeApiService,
        { provide: API_BASE_URL, useValue: baseUrl }
      ]
    });

    service = TestBed.inject(EmployeeApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch an employee by id', () => {
    const mockEmployee: Employee = {
      id: '123e4567-e89b-12d3-a456-426614174000',
      employeeNumber: 'EMP001',
      firstName: 'Ada',
      lastName: 'Lovelace',
      email: 'ada@example.com',
      countryCode: 'US',
      jobTitle: 'Scientist',
      department: 'R&D',
      joinedAt: '2025-01-01',
      terminatedAt: null
    };

    service.getEmployee(mockEmployee.id).subscribe(emp => {
      expect(emp).toEqual(mockEmployee);
    });

    const req = httpMock.expectOne(`${baseUrl}/employees/${mockEmployee.id}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockEmployee);
  });

  it('should create an employee', () => {
    const request: CreateEmployeeRequest = {
      employeeNumber: 'EMP002',
      firstName: 'Grace',
      lastName: 'Hopper',
      email: 'grace@example.com',
      countryCode: 'US',
      jobTitle: 'Rear Admiral',
      department: 'Systems',
      joinedAt: '2025-02-01'
    };

    const mockResponse: Employee = {
      id: '223e4567-e89b-12d3-a456-426614174001',
      ...request,
      terminatedAt: null
    };

    service.createEmployee(request).subscribe(emp => {
      expect(emp.id).toBe(mockResponse.id);
      expect(emp.employeeNumber).toBe('EMP002');
    });

    const req = httpMock.expectOne(`${baseUrl}/employees`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should update employee details', () => {
    const empId = '123e4567-e89b-12d3-a456-426614174000';
    const request: UpdateEmployeeRequest = {
      firstName: 'Ada',
      lastName: 'King',
      email: 'ada.king@example.com',
      countryCode: 'GB',
      jobTitle: 'Lead Scientist'
    };

    service.updateEmployee(empId, request).subscribe(emp => {
      expect(emp.lastName).toBe('King');
    });

    const req = httpMock.expectOne(`${baseUrl}/employees/${empId}`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: empId, employeeNumber: 'EMP001', ...request, joinedAt: '2025-01-01', terminatedAt: null });
  });

  it('should delete employee by id', () => {
    const empId = '123e4567-e89b-12d3-a456-426614174000';

    service.deleteEmployee(empId).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/employees/${empId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
