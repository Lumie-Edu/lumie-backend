-- =====================================================================
-- V22: Introduce public.owner_directory
--
-- Purpose: provide a lightweight, RLS-EXEMPT identity-routing table
-- that maps a login id to its tenant. This resolves the login
-- chicken-and-egg problem: public.users is protected by RLS and cannot
-- be queried before a tenant context is established.  owner_directory
-- is intentionally a platform table — like tenants/plans — readable by
-- lumie_app with no app.tenant_id set.
--
-- NO RLS: do NOT add ENABLE/FORCE ROW LEVEL SECURITY or any policy on
-- this table.  The NOSUPERUSER NOBYPASSRLS runtime role lumie_app must
-- be able to read it unconditionally for pre-auth tenant routing.
--
-- Rollback: DROP TABLE IF EXISTS public.owner_directory; (additive only,
-- no existing columns modified or dropped).
-- =====================================================================

-- 1. Create the owner directory table (platform/identity-routing, NOT tenant-scoped)
create table if not exists public.owner_directory (
    -- id is application-supplied: the adapter inserts the OWNER's users.id explicitly.
    -- do NOT use serial/bigserial/identity — the PK value is determined by the caller.
    id              bigint          not null,
    user_login_id   varchar(50)     not null,
    tenant_id       bigint          not null references tenants(id) on delete cascade,
    email           varchar(255),
    created_at      timestamptz     not null default now(),
    updated_at      timestamptz     not null default now(),
    constraint owner_directory_pkey primary key (id),
    constraint owner_directory_user_login_id_key unique (user_login_id)
);

-- 2. Index for tenant-scoped lookups. (user_login_id is already indexed by
--    the UNIQUE constraint above, so no separate index is needed for it.)
create index if not exists idx_owner_directory_tenant_id
    on public.owner_directory (tenant_id);

-- 3. Backfill: copy all existing OWNER rows from public.users.
--    This migration runs as the `postgres` migrator (SUPERUSER) which bypasses
--    RLS on public.users, so all OWNER rows are visible regardless of tenant context.
--    ON CONFLICT (id) DO NOTHING makes this idempotent on repeated apply.
insert into public.owner_directory (id, user_login_id, tenant_id, email)
select id, user_login_id, tenant_id, email
from public.users
where role = 'OWNER'
on conflict (id) do nothing;

-- 4. Grant runtime role full DML access (no RLS policy guards this table)
grant select, insert, update, delete on public.owner_directory to lumie_app;
