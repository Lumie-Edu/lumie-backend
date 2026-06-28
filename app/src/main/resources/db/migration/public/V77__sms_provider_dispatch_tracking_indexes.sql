-- @flyway:nonTransactional
-- CREATE INDEX CONCURRENTLY cannot run inside a transaction (Postgres restriction).

set lock_timeout = '5s';
set statement_timeout = '30s';

create index concurrently if not exists idx_sms_messages_tenant_provider_group_id
    on sms_messages (tenant_id, provider_group_id)
    where provider_group_id is not null;

create index concurrently if not exists idx_sms_messages_tenant_pending_dispatch
    on sms_messages (tenant_id, scheduled_at, dispatch_attempt_count)
    where status = 'PENDING';
