-- Remove the fail-closed audience sentinel after all rows have been explicitly
-- classified or deleted and all writers require ALL/TARGETED. Deploy only after
-- draining application versions that can omit audience_scope; old V112 history
-- remains immutable.
set local lock_timeout = '5s';
set local statement_timeout = '60s';

lock table announcements in access exclusive mode;

do $$
declare
    legacy_count bigint;
begin
    select count(*)
      into legacy_count
      from announcements
     where audience_scope = 'LEGACY_UNSPECIFIED';

    if legacy_count > 0 then
        raise exception 'V141 abort: % announcement(s) still use LEGACY_UNSPECIFIED; classify or delete them first',
            legacy_count;
    end if;
end $$;

drop trigger if exists trg_promote_legacy_announcement_audience_scope
    on announcement_class_targets;

drop function if exists promote_legacy_announcement_audience_scope();

alter table announcements
    alter column audience_scope drop default;

alter table announcements
    drop constraint if exists announcements_audience_scope_check;

alter table announcements
    add constraint announcements_audience_scope_check
    check (audience_scope in ('ALL', 'TARGETED'));

comment on column announcements.audience_scope is
    'Explicit student audience: ALL or TARGETED.';
