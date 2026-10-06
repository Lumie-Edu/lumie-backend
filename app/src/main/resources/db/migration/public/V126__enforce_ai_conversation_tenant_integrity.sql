-- ============================================================================
-- V126: enforce AI conversation tenant integrity
--
-- Purpose:
--   Enforce same-tenant ownership through the full AI chat hierarchy:
--   users -> ai_conversations -> ai_chat_messages. Deleting a user cascades
--   to that user's conversations and then to their messages.
--
-- Deploy order:
--   1. Back up and manually remove the audited development orphan AI
--      conversations and their messages before applying this migration.
--   2. Apply this migration only after confirming no orphan or cross-tenant
--      AI conversation/message relationship remains. Production has no
--      audited orphan rows and is not repaired by this migration.
--
-- Forward-only:
--   This migration does not rewrite or delete AI chat records. It fails
--   closed on an orphan or cross-tenant reference, adds tenant-safe foreign
--   keys, validates them, and only then replaces the legacy ID-only message
--   foreign key while preserving ON DELETE CASCADE.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Composite foreign keys require an exact unique parent key. V121 already
-- provides idx_users_id_tenant_id for users(id, tenant_id).
create unique index if not exists idx_ai_conversations_id_tenant_id
    on ai_conversations (id, tenant_id);

-- Do not allow an identically numbered parent in another tenant to satisfy a
-- relationship. Historical inconsistencies must be handled before contract.
do $$
begin
    if exists (
        select 1
          from ai_conversations conversation
          left join users user_account
            on user_account.id = conversation.user_id
           and user_account.tenant_id = conversation.tenant_id
         where user_account.id is null
    ) then
        raise exception
            'cannot enforce AI conversation tenant integrity: an orphan or cross-tenant user reference remains';
    end if;

    if exists (
        select 1
          from ai_chat_messages message
          left join ai_conversations conversation
            on conversation.id = message.conversation_id
           and conversation.tenant_id = message.tenant_id
         where conversation.id is null
    ) then
        raise exception
            'cannot enforce AI message tenant integrity: an orphan or cross-tenant conversation reference remains';
    end if;
end $$;

-- Add the new constraints as NOT VALID first; their validation below is an
-- explicit transition gate before the legacy ID-only message FK is removed.
do $$
begin
    if not exists (
        select 1
          from pg_constraint
         where conname = 'ai_conversations_user_tenant_fkey'
           and conrelid = 'ai_conversations'::regclass
    ) then
        alter table ai_conversations
            add constraint ai_conversations_user_tenant_fkey
            foreign key (user_id, tenant_id)
            references users (id, tenant_id)
            on delete cascade
            not valid;
    end if;

    if not exists (
        select 1
          from pg_constraint
         where conname = 'ai_chat_messages_conversation_tenant_fkey'
           and conrelid = 'ai_chat_messages'::regclass
    ) then
        alter table ai_chat_messages
            add constraint ai_chat_messages_conversation_tenant_fkey
            foreign key (conversation_id, tenant_id)
            references ai_conversations (id, tenant_id)
            on delete cascade
            not valid;
    end if;
end $$;

-- Validate only the intended tenant-safe foreign keys. A manually altered
-- database must fail instead of contracting an unintended relationship.
do $$
declare
    constraint_is_valid boolean;
begin
    select convalidated
      into constraint_is_valid
      from pg_constraint
     where conname = 'ai_conversations_user_tenant_fkey'
       and conrelid = 'ai_conversations'::regclass
       and confrelid = 'users'::regclass
       and contype = 'f'
       and conkey = array[
           (select attnum from pg_attribute
             where attrelid = 'ai_conversations'::regclass
               and attname = 'user_id'
               and not attisdropped),
           (select attnum from pg_attribute
             where attrelid = 'ai_conversations'::regclass
               and attname = 'tenant_id'
               and not attisdropped)
       ]::smallint[]
       and confkey = array[
           (select attnum from pg_attribute
             where attrelid = 'users'::regclass
               and attname = 'id'
               and not attisdropped),
           (select attnum from pg_attribute
             where attrelid = 'users'::regclass
               and attname = 'tenant_id'
               and not attisdropped)
       ]::smallint[]
       and confdeltype = 'c';

    if not found then
        raise exception
            'cannot enforce AI conversation tenant integrity: expected user foreign key is missing';
    end if;

    if not constraint_is_valid then
        alter table ai_conversations
            validate constraint ai_conversations_user_tenant_fkey;
    end if;

    select convalidated
      into constraint_is_valid
      from pg_constraint
     where conname = 'ai_chat_messages_conversation_tenant_fkey'
       and conrelid = 'ai_chat_messages'::regclass
       and confrelid = 'ai_conversations'::regclass
       and contype = 'f'
       and conkey = array[
           (select attnum from pg_attribute
             where attrelid = 'ai_chat_messages'::regclass
               and attname = 'conversation_id'
               and not attisdropped),
           (select attnum from pg_attribute
             where attrelid = 'ai_chat_messages'::regclass
               and attname = 'tenant_id'
               and not attisdropped)
       ]::smallint[]
       and confkey = array[
           (select attnum from pg_attribute
             where attrelid = 'ai_conversations'::regclass
               and attname = 'id'
               and not attisdropped),
           (select attnum from pg_attribute
             where attrelid = 'ai_conversations'::regclass
               and attname = 'tenant_id'
               and not attisdropped)
       ]::smallint[]
       and confdeltype = 'c';

    if not found then
        raise exception
            'cannot enforce AI message tenant integrity: expected conversation foreign key is missing';
    end if;

    if not constraint_is_valid then
        alter table ai_chat_messages
            validate constraint ai_chat_messages_conversation_tenant_fkey;
    end if;
end $$;

-- Replace the ID-only message FK only after the tenant-safe FK is validated.
-- User deletion now cascades conversations, then messages; direct conversation
-- deletion continues to cascade messages as before.
alter table if exists ai_chat_messages
    drop constraint if exists ai_chat_messages_conversation_id_fkey;
