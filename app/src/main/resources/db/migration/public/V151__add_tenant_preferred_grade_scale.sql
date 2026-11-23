ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS preferred_grade_scale VARCHAR(20) NOT NULL DEFAULT 'NINE_GRADE';

COMMENT ON COLUMN tenants.preferred_grade_scale IS
    'Tenant-wide display preference for grade distribution charts and grade-scale controls.';
