export interface CompensationRecord {
  id: string;
  employeeId: string;
  annualSalary: number;
  currencyCode: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  createdAt: string;
}

export interface CreateCompensationRequest {
  annualSalary: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
}

export interface ChangeSalaryRequest {
  annualSalary: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
}
