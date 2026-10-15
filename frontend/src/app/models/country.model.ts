export interface Country {
  code: string;
  name: string;
  currencyCode: string;
}

/**
 * System supported countries.
 *
 * Limitation note: The backend currently does not expose a public GET /api/v1/countries
 * endpoint. To avoid inventing non-existent backend endpoints while maintaining a consistent
 * user experience for country filtering and employee creation, the supported countries are
 * defined based on current schema and seed definitions (US, GB).
 */
export const SUPPORTED_COUNTRIES: readonly Country[] = [
  { code: 'US', name: 'United States', currencyCode: 'USD' },
  { code: 'GB', name: 'United Kingdom', currencyCode: 'GBP' }
];

