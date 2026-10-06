-- ============================================================================
-- V122: retire legacy textbooks
--
-- Deploy order:
--   1. Deploy the chatbot runtime that no longer advertises or queries the
--      retired public.textbooks table. Every old backend/worker pod must be
--      retired before this contract migration runs.
--   2. Preserve the original files outside this table. The canonical textbook
--      product is file_metadata/textbook_folders; no legacy textbook row is
--      a file ownership record.
--   3. Confirm production still contains only the audited tenant-97 IDs 1-15,
--      or that an environment is already empty.
--   4. Apply this migration. Do not roll it out while an earlier chatbot
--      runtime can receive traffic.
--
-- Forward-only and destructive:
--   This contract deletes only the audited tenant-97 legacy metadata rows,
--   then removes their unreferenced table. It accepts an empty environment
--   (development) or exactly the audited IDs 1-15 for tenant 97 (production);
--   any new, other-tenant, or partially changed legacy data fails closed. File metadata, folders,
--   objects, chat history, and every other table are intentionally retained.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
declare
    legacy_row_count bigint;
    tenant_97_row_count bigint;
begin
    if to_regclass('public.textbooks') is null then
        return;
    end if;

    select count(*), count(*) filter (where tenant_id = 97)
      into legacy_row_count, tenant_97_row_count
      from textbooks;

    if legacy_row_count not in (0, 15)
       or legacy_row_count <> tenant_97_row_count
       or exists (
           select 1
           from textbooks
           where tenant_id = 97
             and id not between 1 and 15
       ) then
        raise exception
            'cannot retire legacy textbooks: expected zero rows or exactly audited tenant-97 IDs 1-15, found % total and % tenant-97 rows',
            legacy_row_count, tenant_97_row_count;
    end if;

    -- Production's 15 rows are legacy metadata only; original files have
    -- already been preserved outside public.textbooks. Keeping this in the
    -- guarded block also makes a manually retired environment safe to migrate.
    delete from textbooks
     where tenant_id = 97
       and id between 1 and 15;

    if exists (select 1 from textbooks) then
        raise exception
            'cannot retire legacy textbooks: rows remain after audited cleanup';
    end if;
end $$;

-- No CASCADE: an unexpected FK, view, or other database dependency must abort
-- the contract rather than silently removing a consumer.
drop table if exists textbooks;
