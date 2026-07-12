update tenants
set hidden_sidebar_item_ids = coalesce(
    (
        select jsonb_agg(item)
        from jsonb_array_elements(hidden_sidebar_item_ids) as item
        where item <> '"schedules"'::jsonb
    ),
    '[]'::jsonb
)
where hidden_sidebar_item_ids ? 'schedules';

-- Forward-only contract migration: counseling schedules/reservations are no
-- longer exposed by runtime code, OpenAPI, or frontend clients. Historical data
-- is intentionally discarded after that cutover; deploy application code that
-- removes the feature before applying this drop.
--
-- V66 preserved user-history on student delete by nulling reservation.student_id
-- from a trigger function body. PostgreSQL does not dependency-track table
-- references inside PL/pgSQL bodies, so rewrite the function before dropping the
-- table to keep future student deletes working.
create or replace function mark_exam_results_student_deleted()
returns trigger
language plpgsql
as $$
begin
    update exam_results
       set student_deleted = true,
           phone_number = null
     where student_id = old.id;

    return old;
end;
$$;

drop table if exists counseling_reservations cascade;
drop table if exists counseling_schedules cascade;
