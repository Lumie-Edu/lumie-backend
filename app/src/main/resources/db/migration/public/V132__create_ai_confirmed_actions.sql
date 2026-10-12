-- Prepared chatbot writes are tenant-bound and cannot be replayed through a
-- different conversation, message, or request key. This is additive to the
-- legacy pending_action JSON; old pending actions are not migrated to executable actions.
CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_chat_messages_id_tenant_id
    ON ai_chat_messages (id, tenant_id);

CREATE TABLE IF NOT EXISTS ai_confirmed_actions (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    message_id BIGINT,
    tool_call_id VARCHAR(100) NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_version INTEGER NOT NULL CHECK (tool_version > 0),
    arguments JSONB NOT NULL,
    preview JSONB NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN (
        'PREPARED', 'CANCELLED', 'EXECUTING', 'SUCCEEDED', 'FAILED', 'UNKNOWN_OUTCOME'
    )),
    idempotency_key VARCHAR(150),
    result JSONB,
    last_error_code VARCHAR(100),
    expires_at TIMESTAMPTZ NOT NULL,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ai_confirmed_actions_user_tenant_fkey
        FOREIGN KEY (user_id, tenant_id) REFERENCES users (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT ai_confirmed_actions_conversation_tenant_fkey
        FOREIGN KEY (conversation_id, tenant_id)
        REFERENCES ai_conversations (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT ai_confirmed_actions_message_tenant_fkey
        FOREIGN KEY (message_id, tenant_id)
        REFERENCES ai_chat_messages (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT ai_confirmed_actions_tool_call_unique
        UNIQUE (tenant_id, conversation_id, tool_call_id),
    CONSTRAINT ai_confirmed_actions_idempotency_unique
        UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_ai_confirmed_actions_conversation_created
    ON ai_confirmed_actions (tenant_id, conversation_id, created_at DESC);

ALTER TABLE ai_confirmed_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_confirmed_actions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ai_confirmed_actions
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);
