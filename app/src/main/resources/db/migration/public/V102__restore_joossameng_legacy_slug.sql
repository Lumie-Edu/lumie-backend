-- Forward-only correction for environments where V100 renamed the existing
-- joossameng tenant to inst-joossameng. Keep the established immutable slug so
-- pre-session-migration JWTs and opaque sessions remain valid through their
-- natural expiry. Production must receive the slug-safe V100 before V101; in
-- that case this migration is intentionally a no-op.

set local lock_timeout = '5s';
set local statement_timeout = '2min';

do $$
begin
    if exists (select 1 from tenants where slug = 'joossameng')
       and exists (select 1 from tenants where slug = 'inst-joossameng') then
        raise exception 'cannot restore joossameng slug: both joossameng and inst-joossameng exist';
    end if;

    update tenants
    set slug = 'joossameng'
    where slug = 'inst-joossameng';
end $$;

-- Restore denormalized values only when V100 changed them. These updates are
-- idempotent and preserve the original slug for the existing tenant.
update subscriptions
set tenant_slug = 'joossameng'
where tenant_slug = 'inst-joossameng';

update ai_scheduled_tasks
set tenant_slug = 'joossameng'
where tenant_slug = 'inst-joossameng';

update omr_grading_jobs
set tenant_slug = 'joossameng'
where tenant_slug = 'inst-joossameng';

update report_generation_jobs
set tenant_slug = 'joossameng'
where tenant_slug = 'inst-joossameng';
