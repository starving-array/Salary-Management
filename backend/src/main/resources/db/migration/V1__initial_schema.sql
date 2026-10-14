-- V1__initial_schema.sql
-- Salary Management System Initial Schema

-- 1. Country table
CREATE TABLE country (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    CONSTRAINT uq_country_name UNIQUE (name),
    CONSTRAINT chk_country_code_upper CHECK (code = UPPER(code)),
    CONSTRAINT chk_country_currency_upper CHECK (currency_code = UPPER(currency_code))
);

-- 2. Employee table
CREATE TABLE employee (
    id UUID PRIMARY KEY,
    employee_number VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    joined_at DATE NOT NULL,
    terminated_at DATE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_employee_number UNIQUE (employee_number),
    CONSTRAINT uq_employee_email UNIQUE (email),
    CONSTRAINT fk_employee_country FOREIGN KEY (country_code) REFERENCES country (code) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_dates CHECK (terminated_at IS NULL OR terminated_at >= joined_at)
);

CREATE INDEX idx_employee_country_code ON employee (country_code);
CREATE INDEX idx_employee_job_title ON employee (job_title);
CREATE INDEX idx_employee_deleted_at ON employee (deleted_at);

-- 3. Compensation Record table
CREATE TABLE compensation_record (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    annual_salary NUMERIC(15, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_compensation_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE RESTRICT,
    CONSTRAINT chk_compensation_salary_positive CHECK (annual_salary > 0),
    CONSTRAINT chk_compensation_effective_dates CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- Partial unique index ensuring at most one open-ended compensation record per employee
CREATE UNIQUE INDEX uq_compensation_open_ended ON compensation_record (employee_id) WHERE effective_to IS NULL;

-- Index for employee compensation history and effective date lookups
CREATE INDEX idx_compensation_employee_dates ON compensation_record (employee_id, effective_from, effective_to);
