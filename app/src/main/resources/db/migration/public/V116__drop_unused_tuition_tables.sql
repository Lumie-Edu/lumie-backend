-- ============================================================================
-- V116: drop unused academy tuition tables
--
-- Deploy order:
--   1. Deploy and retire every backend/worker pod that reads or writes the
--      academy tuition and independent guardian domain.
--   2. Reconfirm these six tables are empty in every target environment.
--   3. Apply this contract migration. Do not roll out this migration while an
--      earlier runtime can still receive traffic.
--
-- Forward-only and destructive:
--   The application-owned academy tuition feature is retired. The migration
--   fails before any DDL if any target table has a row; it never deletes
--   tuition, payment, guardian, or receipt data.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
declare
    table_name text;
    table_has_rows boolean;
begin
    foreach table_name in array array[
        'cash_receipts',
        'tuition_payments',
        'tuition_invoices',
        'merchant_profiles',
        'student_guardians',
        'guardians'
    ] loop
        if to_regclass(format('public.%I', table_name)) is not null then
            execute format('select exists (select 1 from public.%I)', table_name)
                into table_has_rows;
            if table_has_rows then
                raise exception
                    'cannot drop unused academy tuition table %: table contains rows', table_name;
            end if;
        end if;
    end loop;
end $$;

drop table if exists cash_receipts;
drop table if exists tuition_payments;
drop table if exists tuition_invoices;
drop table if exists merchant_profiles;
drop table if exists student_guardians;
drop table if exists guardians;
