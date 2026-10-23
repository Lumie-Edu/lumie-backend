-- A confirmed chat action can be reconciled to the SMS record after a crash.
-- Keep this as a nullable source key rather than a cascading FK: SMS delivery
-- history must survive any later retention cleanup of AI conversations/actions.
ALTER TABLE sms_messages
    ADD COLUMN IF NOT EXISTS source_action_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS idx_sms_messages_tenant_source_action
    ON sms_messages (tenant_id, source_action_id)
    WHERE source_action_id IS NOT NULL;
