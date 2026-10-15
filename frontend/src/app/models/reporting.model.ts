export interface CountrySalarySummary {
  countryCode: string;
  countryName: string;
  currencyCode: string;
  asOfDate: string;
  eligibleEmployeeCount: number;
  minSalary: number | null;
  maxSalary: number | null;
  averageSalary: number | null;
}

export interface JobTitleSalary {
  jobTitle: string;
  employeeCount: number;
  averageSalary: number | null;
}

export interface CountryJobTitleSalarySummary {
  countryCode: string;
  countryName: string;
  currencyCode: string;
  asOfDate: string;
  totalEligibleEmployees: number;
  jobTitles: JobTitleSalary[];
}
