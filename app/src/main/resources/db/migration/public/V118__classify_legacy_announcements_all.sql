-- ============================================================================
-- V118: classify selected legacy announcements as academy-wide
--
-- Scope:
--   The academy owner explicitly classified the four joossameng announcements
--   below as academy-wide. Other LEGACY_UNSPECIFIED announcements deliberately
--   remain fail-closed until separately classified.
--
-- Forward-only:
--   This backfill changes only audience metadata. It does not create or remove
--   class targets, and it increments optimistic-lock versions only for rows
--   that change from LEGACY_UNSPECIFIED to ALL.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Fail closed if a selected production row no longer has the evidence this
-- manual classification relies on. Missing rows are intentionally tolerated:
-- development does not contain these production announcement IDs.
do $$
declare
    joossameng_tenant_id bigint;
begin
    select id
      into joossameng_tenant_id
      from tenants
     where slug = 'joossameng';

    -- Keep the checked audience/target state stable through the UPDATE below.
    perform 1
      from announcements
     where tenant_id = joossameng_tenant_id
       and id in (5, 16, 23, 76)
     for update;

    if joossameng_tenant_id is not null and exists (
        select 1
          from announcements announcement
         where announcement.tenant_id = joossameng_tenant_id
           and announcement.id in (5, 16, 23, 76)
           and (
               (announcement.id = 5 and btrim(announcement.announcement_title) <> 'Q&A 이용안내')
               or (announcement.id = 16 and btrim(announcement.announcement_title) <> '주혜연T - 개념인강 안내 (★6모 기준 70점 이하 권장)')
               or (announcement.id = 23 and btrim(announcement.announcement_title) <> '순삽 2주완성 교재 정오표')
               or (announcement.id = 76 and btrim(announcement.announcement_title) <> 'FINAL 1주차 분석지')
               or announcement.audience_scope not in ('LEGACY_UNSPECIFIED', 'ALL')
               or exists (
                    select 1
                     from announcement_class_targets target
                    where target.announcement_id = announcement.id
                      and target.tenant_id = announcement.tenant_id
               )
           )
    ) then
        raise exception
            'V118 abort: a selected joossameng announcement has unexpected title, audience scope, or class targets';
    end if;
end $$;

update announcements announcement
   set audience_scope = 'ALL',
       updated_at = now(),
       version = announcement.version + 1
  from tenants tenant
 where tenant.id = announcement.tenant_id
   and tenant.slug = 'joossameng'
   and announcement.audience_scope = 'LEGACY_UNSPECIFIED'
   and (
       (announcement.id = 5 and btrim(announcement.announcement_title) = 'Q&A 이용안내')
       or (announcement.id = 16 and btrim(announcement.announcement_title) = '주혜연T - 개념인강 안내 (★6모 기준 70점 이하 권장)')
       or (announcement.id = 23 and btrim(announcement.announcement_title) = '순삽 2주완성 교재 정오표')
       or (announcement.id = 76 and btrim(announcement.announcement_title) = 'FINAL 1주차 분석지')
   );
