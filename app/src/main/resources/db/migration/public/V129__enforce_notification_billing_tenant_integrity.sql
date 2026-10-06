-- ============================================================================
-- V129: enforce notification and billing tenant integrity
--
-- Purpose:
--   Replace the remaining ID-only notification and billing foreign keys with
--   same-tenant composite foreign keys. Existing deletion semantics remain
--   unchanged, including RESTRICT on financial records and SET NULL only on
--   the nullable reference column.
--
-- Forward-only:
--   No notification or billing row is rewritten or deleted. The migration
--   fails closed on orphan/cross-tenant data, validates each exact composite
--   key before it removes the prior ID-only constraint.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
declare
    parent_table text;
    index_name text;
    parent_key_exists boolean;
begin
    for parent_table, index_name in
        select * from (values
            ('billing_keys', 'idx_billing_keys_id_tenant_id'),
            ('invoices', 'idx_invoices_id_tenant_id'),
            ('sms_templates', 'idx_sms_templates_id_tenant_id'),
            ('staff', 'idx_staff_id_tenant_id'),
            ('subscriptions', 'idx_subscriptions_id_tenant_id')
        ) as parent_keys(parent_table, index_name)
    loop
        select exists (
            select 1
              from pg_index index_definition
             where index_definition.indrelid = ('public.' || parent_table)::regclass
               and index_definition.indisunique
               and index_definition.indisvalid
               and index_definition.indpred is null
               and index_definition.indnkeyatts = 2
               and index_definition.indkey[0] = (
                   select attnum from pg_attribute
                    where attrelid = ('public.' || parent_table)::regclass
                      and attname = 'id' and not attisdropped
               )
               and index_definition.indkey[1] = (
                   select attnum from pg_attribute
                    where attrelid = ('public.' || parent_table)::regclass
                      and attname = 'tenant_id' and not attisdropped
               )
        ) into parent_key_exists;

        if not parent_key_exists then
            if to_regclass('public.' || index_name) is not null then
                raise exception
                    'cannot enforce tenant integrity: index % exists but is not an exact unique (id, tenant_id) key on %',
                    index_name, parent_table;
            end if;
            execute format(
                'create unique index if not exists %I on %I (id, tenant_id)',
                index_name,
                parent_table
            );
        end if;
    end loop;
end $$;

do $$
declare
    child_table text;
    parent_table text;
    reference_column text;
    mismatch_exists boolean;
begin
    for child_table, parent_table, reference_column in
        select * from (values
            ('sms_messages', 'staff', 'sender_id'),
            ('sms_messages', 'sms_templates', 'template_id'),
            ('invoices', 'subscriptions', 'subscription_id'),
            ('subscriptions', 'billing_keys', 'billing_key_id'),
            ('tax_invoices', 'invoices', 'invoice_id')
        ) as relationships(child_table, parent_table, reference_column)
    loop
        execute format(
            'select exists (
                select 1
                  from %I child_row
                  left join %I parent_row
                    on parent_row.id = child_row.%I
                   and parent_row.tenant_id = child_row.tenant_id
                 where child_row.%I is not null
                   and parent_row.id is null
            )',
            child_table,
            parent_table,
            reference_column,
            reference_column
        ) into mismatch_exists;

        if mismatch_exists then
            raise exception
                'cannot enforce tenant integrity: %.% has an orphan or cross-tenant reference to %',
                child_table, reference_column, parent_table;
        end if;
    end loop;
end $$;

do $$
declare
    child_table text;
    parent_table text;
    reference_column text;
    constraint_name text;
    delete_clause text;
begin
    for child_table, parent_table, reference_column, constraint_name, delete_clause in
        select * from (values
            ('sms_messages', 'staff', 'sender_id', 'sms_messages_sender_tenant_fkey', 'set null (sender_id)'),
            ('sms_messages', 'sms_templates', 'template_id', 'sms_messages_template_tenant_fkey', 'set null (template_id)'),
            ('invoices', 'subscriptions', 'subscription_id', 'invoices_subscription_tenant_fkey', 'restrict'),
            ('subscriptions', 'billing_keys', 'billing_key_id', 'subscriptions_billing_key_tenant_fkey', 'set null (billing_key_id)'),
            ('tax_invoices', 'invoices', 'invoice_id', 'tax_invoices_invoice_tenant_fkey', 'restrict')
        ) as relationships(child_table, parent_table, reference_column, constraint_name, delete_clause)
    loop
        if not exists (
            select 1 from pg_constraint
             where conname = constraint_name
               and conrelid = ('public.' || child_table)::regclass
        ) then
            execute format(
                'alter table %I add constraint %I foreign key (%I, tenant_id) references %I (id, tenant_id) on delete %s not valid',
                child_table,
                constraint_name,
                reference_column,
                parent_table,
                delete_clause
            );
        end if;
    end loop;
end $$;

do $$
declare
    child_table text;
    parent_table text;
    reference_column text;
    constraint_name text;
    expected_delete_action "char";
    constraint_is_valid boolean;
begin
    for child_table, parent_table, reference_column, constraint_name, expected_delete_action in
        select * from (values
            ('sms_messages', 'staff', 'sender_id', 'sms_messages_sender_tenant_fkey', 'n'::"char"),
            ('sms_messages', 'sms_templates', 'template_id', 'sms_messages_template_tenant_fkey', 'n'::"char"),
            ('invoices', 'subscriptions', 'subscription_id', 'invoices_subscription_tenant_fkey', 'r'::"char"),
            ('subscriptions', 'billing_keys', 'billing_key_id', 'subscriptions_billing_key_tenant_fkey', 'n'::"char"),
            ('tax_invoices', 'invoices', 'invoice_id', 'tax_invoices_invoice_tenant_fkey', 'r'::"char")
        ) as relationships(child_table, parent_table, reference_column, constraint_name, expected_delete_action)
    loop
        execute format(
            'select foreign_key.convalidated
               from pg_constraint foreign_key
              where foreign_key.conname = %L
                and foreign_key.conrelid = %L::regclass
                and foreign_key.confrelid = %L::regclass
                and foreign_key.contype = ''f''
                and foreign_key.confdeltype = %L::"char"
                and foreign_key.conkey = array[
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = %L and not attisdropped),
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = ''tenant_id'' and not attisdropped)
                ]::smallint[]
                and foreign_key.confkey = array[
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = ''id'' and not attisdropped),
                    (select attnum from pg_attribute where attrelid = %L::regclass and attname = ''tenant_id'' and not attisdropped)
                ]::smallint[]
                and (
                    %L::"char" <> ''n''::"char"
                    or position(format(''ON DELETE SET NULL (%%I)'', %L) in pg_get_constraintdef(foreign_key.oid)) > 0
                )',
            constraint_name,
            'public.' || child_table,
            'public.' || parent_table,
            expected_delete_action,
            'public.' || child_table,
            reference_column,
            'public.' || child_table,
            'public.' || parent_table,
            'public.' || parent_table,
            expected_delete_action,
            reference_column
        ) into constraint_is_valid;

        if constraint_is_valid is null then
            raise exception
                'cannot enforce tenant integrity: expected composite foreign key % is missing or has a different definition',
                constraint_name;
        end if;

        if not constraint_is_valid then
            execute format('alter table %I validate constraint %I', child_table, constraint_name);
        end if;
    end loop;
end $$;

alter table if exists sms_messages
    drop constraint if exists sms_messages_sender_id_fkey,
    drop constraint if exists sms_messages_template_id_fkey;

alter table if exists invoices
    drop constraint if exists invoices_subscription_id_fkey;

alter table if exists subscriptions
    drop constraint if exists subscriptions_billing_key_id_fkey;

alter table if exists tax_invoices
    drop constraint if exists tax_invoices_invoice_id_fkey;
