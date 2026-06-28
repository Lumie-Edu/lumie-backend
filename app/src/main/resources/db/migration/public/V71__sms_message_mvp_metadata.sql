-- Add SMS MVP scheduling, cancellation, type, and pricing metadata to sms_messages.

set local lock_timeout = '5s';
set local statement_timeout = '30s';

alter table if exists sms_messages
    add column if not exists scheduled_at timestamptz,
    add column if not exists cancelled_at timestamptz,
    add column if not exists message_type varchar(10) not null default 'SMS',
    add column if not exists estimated_segments integer not null default 1,
    add column if not exists unit_price integer not null default 0,
    add column if not exists total_price integer not null default 0,
    add column if not exists version bigint not null default 0;

create index if not exists idx_sms_messages_tenant_status_scheduled_at
    on sms_messages (tenant_id, status, scheduled_at);

create index if not exists idx_sms_messages_tenant_pending_scheduled_at
    on sms_messages (tenant_id, scheduled_at)
    where status = 'PENDING'
      and scheduled_at is not null
      and cancelled_at is null;
