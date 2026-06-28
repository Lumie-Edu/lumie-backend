-- Add provider dispatch tracking for live SMS integrations.

alter table if exists sms_messages
    add column if not exists provider varchar(30),
    add column if not exists provider_group_id varchar(80),
    add column if not exists dispatch_attempt_count integer not null default 0,
    add column if not exists last_provider_status varchar(50),
    add column if not exists last_provider_error text,
    add column if not exists dispatched_at timestamptz,
    add column if not exists confirmed_at timestamptz;

create index if not exists idx_sms_messages_tenant_provider_group_id
    on sms_messages (tenant_id, provider_group_id)
    where provider_group_id is not null;

create index if not exists idx_sms_messages_tenant_pending_dispatch
    on sms_messages (tenant_id, scheduled_at, dispatch_attempt_count)
    where status = 'PENDING';
