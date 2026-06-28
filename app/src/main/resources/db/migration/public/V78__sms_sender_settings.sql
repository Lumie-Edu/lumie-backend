-- Persist tenant-specific SMS sender settings and sender numbers.

set local lock_timeout = '5s';
set local statement_timeout = '30s';

create table if not exists sms_sender_settings (
    id                  bigserial primary key,
    tenant_id           bigint       not null,
    version             bigint       not null default 0,
    provider            varchar(30)  not null,
    sender_number       varchar(20)  not null,
    registration_status varchar(30)  not null,
    rejection_reason    text,
    approved_at         timestamptz,
    rejected_at         timestamptz,
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now(),
    constraint fk_sms_sender_settings_tenant
        foreign key (tenant_id) references tenants(id) on delete cascade,
    constraint uq_sms_sender_settings_tenant_provider unique (tenant_id, provider)
);

alter table if exists sms_sender_settings enable row level security;
alter table if exists sms_sender_settings force row level security;
drop policy if exists tenant_isolation on sms_sender_settings;
create policy tenant_isolation on sms_sender_settings
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on sms_sender_settings to lumie_app;
grant usage, select on sequence sms_sender_settings_id_seq to lumie_app;

alter table if exists sms_messages
    add column if not exists sender_number varchar(20);
