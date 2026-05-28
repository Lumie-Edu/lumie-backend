-- =====================================================================
-- V38: Billing operation lock table (Phase E D2)
--
-- Purpose: tenant-level mutex for in-flight billing operations.
--   When the same tenant initiates a billing operation (subscribe,
--   change-plan) from two browser tabs with two different
--   Idempotency-Keys, the second call must reject fast (409) rather
--   than re-issue a Toss billing key.
--
-- Mechanism: INSERT ... ON CONFLICT DO NOTHING. PK is (tenant_id,
--   operation_type) so concurrent INSERTs serialize at the DB layer.
--   On completion the row is DELETE'd. Stale rows older than 15min
--   are cleaned up by BillingOperationLockCleanupScheduler
--   (defense in depth against crashes).
--
-- Scope: PLATFORM-SCOPED.
-- =====================================================================

create table if not exists public.billing_operation_locks (
    tenant_id        bigint       not null,
    operation_type   varchar(50)  not null,
    idempotency_key  varchar(128) not null,
    acquired_at      timestamptz  not null default now(),
    constraint billing_operation_locks_pkey primary key (tenant_id, operation_type)
);

create index if not exists idx_billing_operation_locks_acquired_at
    on public.billing_operation_locks (acquired_at);

comment on table  public.billing_operation_locks                 is 'Short-lived per-tenant mutex for in-flight billing operations. PK collision = 409 OPERATION_IN_PROGRESS.';
comment on column public.billing_operation_locks.tenant_id       is 'Owning tenant. No FK to tenants(id) by design — rows are ephemeral (typically < 30s, max 15min via cleanup scheduler); a tenant delete + in-flight op race is acceptable (stale row cleaned up shortly).';
comment on column public.billing_operation_locks.operation_type  is 'SUBSCRIBE | CHANGE_PLAN | CANCEL — name of the billing flow being executed.';
comment on column public.billing_operation_locks.idempotency_key is 'Caller Idempotency-Key for debugging which retry won the lock.';
comment on column public.billing_operation_locks.acquired_at     is 'Set on INSERT. Cleanup scheduler deletes rows older than 15min.';

grant select, insert, update, delete on public.billing_operation_locks to lumie_app;
