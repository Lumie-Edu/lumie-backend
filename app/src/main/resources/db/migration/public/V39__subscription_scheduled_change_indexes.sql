-- @flyway:nonTransactional
-- CREATE INDEX CONCURRENTLY cannot run inside a transaction (Postgres restriction).
-- Companion to V37 columns: each partial index backs exactly one query
-- issued by SubscriptionScheduledChangeScheduler (Phase E D6).

-- D6: applyPendingDowngrades — find subscriptions where scheduled_change_at <= now
create index concurrently if not exists idx_subscriptions_scheduled_change_at
    on subscriptions (scheduled_change_at)
    where scheduled_change_at is not null;

-- D6: finalizeCancellations — find cancel-grace subscriptions where next_charge_at <= now
create index concurrently if not exists idx_subscriptions_cancel_grace
    on subscriptions (next_charge_at)
    where cancelled_at is not null;

-- D6: expireTrials — find trial subscriptions (no billing key) where expires_at <= now
create index concurrently if not exists idx_subscriptions_trial_expiry
    on subscriptions (expires_at)
    where billing_key_id is null and expires_at is not null;
