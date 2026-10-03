-- ============================================================================
-- V117: drop unused AI scheduled tasks
--
-- Deploy order:
--   1. Deploy and retire every backend/worker pod that reads or writes the
--      chatbot AI scheduled-task feature.
--   2. Reconfirm ai_scheduled_tasks is empty in every target environment.
--   3. Apply this contract migration. Do not roll out this migration while an
--      earlier runtime can still receive traffic.
--
-- Forward-only and destructive:
--   This removes only the chatbot AI scheduled-task table. It intentionally
--   preserves shared scheduling infrastructure, including ShedLock and all
--   non-chatbot scheduled work.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
declare
    table_has_rows boolean;
begin
    if to_regclass('public.ai_scheduled_tasks') is not null then
        execute 'select exists (select 1 from public.ai_scheduled_tasks)'
            into table_has_rows;
        if table_has_rows then
            raise exception
                'cannot drop unused AI scheduled tasks: ai_scheduled_tasks contains rows';
        end if;
    end if;
end $$;

drop table if exists ai_scheduled_tasks;
