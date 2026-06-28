-- Restore V72 to the historical SMS-message index role.
--
-- Important:
-- The current dev database that already recorded V72 with applied checksum
-- 577680772 still needs a one-time flyway repair after this file is restored
-- to an index-only migration, unless the dev database is recreated/reset.

set local lock_timeout = '5s';
set local statement_timeout = '30s';

create index if not exists idx_sms_messages_tenant_status_scheduled_at
    on sms_messages (tenant_id, status, scheduled_at);

create index if not exists idx_sms_messages_tenant_pending_scheduled_at
    on sms_messages (tenant_id, scheduled_at)
    where status = 'PENDING'
      and scheduled_at is not null
      and cancelled_at is null;
