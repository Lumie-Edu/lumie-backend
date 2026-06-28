-- Add provider dispatch tracking for live SMS integrations.

set local lock_timeout = '5s';
set local statement_timeout = '30s';

alter table if exists sms_messages
    add column if not exists provider varchar(30),
    add column if not exists provider_group_id varchar(80),
    add column if not exists dispatch_attempt_count integer default 0,
    add column if not exists last_provider_status varchar(50),
    add column if not exists last_provider_error text,
    add column if not exists dispatched_at timestamptz,
    add column if not exists confirmed_at timestamptz;
