-- Keep the legacy OWNER staff row as the stable target of existing class and
-- lecture teacher_id references, but stop treating it as an active employee.
-- The owner is represented in the roster from the OWNER user/tenant relation.
-- Do not delete this row: the current assignment schema references staff.id.
DO $$
DECLARE
    owner_staff_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO owner_staff_count
    FROM staff s
    JOIN users u ON u.id = s.user_id AND u.tenant_id = s.tenant_id
    JOIN tenants t ON t.id = s.tenant_id
    WHERE t.slug = 'joossameng'
      AND u.role = 'OWNER';

    IF owner_staff_count > 1 THEN
        RAISE EXCEPTION 'Expected at most one legacy OWNER staff row for joossameng, found %', owner_staff_count;
    END IF;

    UPDATE staff s
    SET is_active = FALSE,
        employment_status = 'INACTIVE',
        updated_at = now()
    FROM users u, tenants t
    WHERE u.id = s.user_id
      AND u.tenant_id = s.tenant_id
      AND t.id = s.tenant_id
      AND t.slug = 'joossameng'
      AND u.role = 'OWNER'
      AND (s.is_active IS DISTINCT FROM FALSE OR s.employment_status <> 'INACTIVE');
END $$;
