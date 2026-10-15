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
