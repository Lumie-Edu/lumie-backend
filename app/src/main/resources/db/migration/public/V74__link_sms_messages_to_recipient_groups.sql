-- Link SMS messages to persisted recipient groups and tighten group ownership
-- to tenant-safe composite foreign keys.

set local lock_timeout = '5s';
set local statement_timeout = '30s';

create unique index if not exists uq_staff_id_tenant
    on staff (id, tenant_id);

create unique index if not exists uq_sms_recipient_groups_id_tenant
    on sms_recipient_groups (id, tenant_id);

alter table if exists sms_recipient_groups
    drop constraint if exists fk_sms_recipient_groups_owner_staff;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_sms_recipient_groups_owner_staff'
          and conrelid = 'sms_recipient_groups'::regclass
    ) then
        alter table sms_recipient_groups
            add constraint fk_sms_recipient_groups_owner_staff
            foreign key (owner_staff_id, tenant_id)
            references staff(id, tenant_id)
            on delete cascade;
    end if;
end
$$;

alter table if exists sms_messages
    add column if not exists recipient_group_id bigint;

alter table if exists sms_messages
    drop constraint if exists fk_sms_messages_recipient_group;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_sms_messages_recipient_group'
          and conrelid = 'sms_messages'::regclass
    ) then
        alter table sms_messages
            add constraint fk_sms_messages_recipient_group
            foreign key (recipient_group_id, tenant_id)
            references sms_recipient_groups (id, tenant_id)
            on delete set null (recipient_group_id);
    end if;
end
$$;

create index if not exists idx_sms_messages_tenant_recipient_group_created_at
    on sms_messages (tenant_id, recipient_group_id, created_at desc)
    where recipient_group_id is not null;
