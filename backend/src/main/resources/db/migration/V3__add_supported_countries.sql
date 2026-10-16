-- Seed essential country reference data required by the application.
INSERT INTO country (code, name, currency_code)
VALUES
    ('US', 'United States', 'USD'),
    ('GB', 'United Kingdom', 'GBP')
ON CONFLICT (code) DO NOTHING;
