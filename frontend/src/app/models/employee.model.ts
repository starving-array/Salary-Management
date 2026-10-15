export interface Employee {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  jobTitle: string;
  department?: string | null;
  joinedAt: string;
  terminatedAt?: string | null;
}

export interface CreateEmployeeRequest {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  jobTitle: string;
  department?: string | null;
  joinedAt: string;
  terminatedAt?: string | null;
}

export interface UpdateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  jobTitle: string;
  department?: string | null;
  terminatedAt?: string | null;
}

export interface EmployeePageResponse {
  content: Employee[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  hasNext: boolean;
}

export interface EmployeeFilterParams {
  search?: string;
  countryCode?: string;
  jobTitle?: string;
  page?: number;
  size?: number;
}

