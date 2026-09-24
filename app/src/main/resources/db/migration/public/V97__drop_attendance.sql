-- This is a forward-only destructive migration. Apply it during the approved
-- maintenance window after the runtime/API removal is deployed.
set local lock_timeout = '5s';
set local statement_timeout = '60s';

update tenants
set hidden_sidebar_item_ids = coalesce(
    (
        select jsonb_agg(item)
        from jsonb_array_elements(hidden_sidebar_item_ids) as item
        where item <> '"attendance"'::jsonb
    ),
    '[]'::jsonb
)
where hidden_sidebar_item_ids ? 'attendance';

delete from sms_templates where category = 'ATTENDANCE';

drop table if exists attendance_records;
drop table if exists attendance_sessions;
