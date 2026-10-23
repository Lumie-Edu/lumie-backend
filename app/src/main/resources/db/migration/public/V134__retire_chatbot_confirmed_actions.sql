-- The chatbot is read-only. The confirmed-action table and SMS source key
-- introduced by V132/V133 are unused; keep those applied migrations intact.
-- Abort rather than discard an action or delivery link created after the
-- inspected dev state. Reversing this schema is otherwise irreversible.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM ai_confirmed_actions) THEN
        RAISE EXCEPTION 'Cannot retire chatbot confirmed actions with existing rows';
    END IF;
    IF EXISTS (SELECT 1 FROM sms_messages WHERE source_action_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Cannot retire chatbot action links with existing SMS rows';
    END IF;
END
$$;

DROP TABLE IF EXISTS ai_confirmed_actions;

DROP INDEX IF EXISTS idx_ai_chat_messages_id_tenant_id;

DROP INDEX IF EXISTS idx_sms_messages_tenant_source_action;
ALTER TABLE sms_messages DROP COLUMN IF EXISTS source_action_id;
