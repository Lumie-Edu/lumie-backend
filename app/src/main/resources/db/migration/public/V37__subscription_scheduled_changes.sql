-- =====================================================================
-- V37: Subscription scheduled changes — columns + status check (Phase E)
--
-- Purpose: infrastructure for downgrade defer + cancel-at-period-end +
-- trial expiry. Adds two new columns to subscriptions so the domain
-- can express sub-states via timestamps/FK rather than enum values.
-- Also drops the dead PENDING value from the status CHECK constraint —
-- PENDING is never set by application code and no live rows carry it.
--
-- Companion: V39 adds the partial indexes that back the daily
-- SubscriptionScheduledChangeScheduler queries. Indexes live in a
-- separate non-transactional migration because CREATE INDEX
-- CONCURRENTLY (required for write-hot subscriptions) cannot run
-- inside a transaction.
--
-- Scope: PLATFORM-SCOPED (no tenant_id filter, no RLS) — same as V28.
--
-- Rollback: forward-only. DROP COLUMN + constraint restore is
-- mechanically possible in a dev environment, but if a pending
-- downgrade is in progress the scheduled_change_at record would be
-- lost and the user's intent silently discarded. Do NOT reverse on a
-- live database that has received data.
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. New columns on subscriptions
--    pending_plan_id     : plan to apply at scheduled_change_at (F-flow).
--                          NULL = no pending change.
--    scheduled_change_at : when pending_plan_id should be applied
--                          (typically = next_charge_at at the moment
--                          of scheduleDowngrade).
-- -----------------------------------------------------------------------
alter table subscriptions
    add column if not exists pending_plan_id     varchar(20) references plans(id) on delete restrict,
    add column if not exists scheduled_change_at timestamptz;

comment on column subscriptions.pending_plan_id is
    'Plan to apply at scheduled_change_at (downgrade defer). NULL = no pending change.';
comment on column subscriptions.scheduled_change_at is
    'When pending_plan_id should be applied (typically = next_charge_at at the moment of scheduleDowngrade).';

-- -----------------------------------------------------------------------
-- 2. Drop dead PENDING value from subscriptions_status_check
--
--    PENDING was defined in V28 but is never set by application code.
--    No live rows carry status = 'PENDING' (verified before migration).
--    PostgreSQL does not support ALTER CHECK CONSTRAINT directly;
--    the canonical forward-only approach is DROP + ADD.
-- -----------------------------------------------------------------------
alter table subscriptions
    drop constraint if exists subscriptions_status_check;

alter table subscriptions
    add constraint subscriptions_status_check
        check (status in ('ACTIVE', 'PAUSED', 'PAST_DUE', 'CANCELLED', 'EXPIRED'));
