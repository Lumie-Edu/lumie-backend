-- Persist reusable SMS recipient groups per staff member.

set local lock_timeout = '5s';
set local statement_timeout = '30s';

create unique index if not exists uq_staff_id_tenant
    on staff (id, tenant_id);

create table if not exists sms_recipient_groups (
    id             bigserial primary key,
    tenant_id      bigint       not null,
    version        bigint       not null default 0,
    owner_staff_id bigint       not null,
    name           varchar(200) not null,
    icon           varchar(50)  not null default 'mic',
    recipients     jsonb        not null,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),
    constraint fk_sms_recipient_groups_tenant
        foreign key (tenant_id) references tenants(id) on delete cascade,
    constraint fk_sms_recipient_groups_owner_staff
        foreign key (owner_staff_id, tenant_id)
        references staff(id, tenant_id)
        on delete cascade
);

create index if not exists idx_sms_recipient_groups_tenant_owner_created_at
    on sms_recipient_groups (tenant_id, owner_staff_id, created_at desc);

alter table sms_recipient_groups enable row level security;
alter table sms_recipient_groups force row level security;
drop policy if exists tenant_isolation on sms_recipient_groups;
create policy tenant_isolation on sms_recipient_groups
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on sms_recipient_groups to lumie_app;
grant usage, select on sequence sms_recipient_groups_id_seq to lumie_app;
