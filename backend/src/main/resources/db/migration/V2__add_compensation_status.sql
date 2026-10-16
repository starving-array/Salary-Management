-- V2__add_compensation_status.sql
-- Add lifecycle status column to compensation_record table

ALTER TABLE compensation_record
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'HISTORICAL';

-- Deterministically backfill existing records based on effective dates relative to CURRENT_DATE
UPDATE compensation_record
SET status = CASE
    WHEN effective_from > CURRENT_DATE THEN 'SCHEDULED'
    WHEN effective_to IS NOT NULL AND effective_to <= CURRENT_DATE THEN 'HISTORICAL'
    ELSE 'CURRENT'
END;

-- Enforce valid lifecycle statuses
ALTER TABLE compensation_record
    ADD CONSTRAINT chk_compensation_status CHECK (status IN ('SCHEDULED', 'CURRENT', 'HISTORICAL'));

-- Indices for status lookups and midnight scheduled transitions
CREATE INDEX idx_compensation_status ON compensation_record (status);
CREATE INDEX idx_compensation_status_effective ON compensation_record (status, effective_from);
CREATE INDEX idx_compensation_emp_status ON compensation_record (employee_id, status);
