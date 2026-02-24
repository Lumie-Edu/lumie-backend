-- V20: Create AI service tables (conversations, messages, scheduled tasks)

-- AI Conversations
CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_active ON ai_conversations(user_id, is_active);

-- AI Chat Messages
CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    tool_call_id VARCHAR(100),
    tool_calls JSONB,
    pending_action JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_conversation_id ON ai_chat_messages(conversation_id);

-- AI Scheduled Tasks
CREATE TABLE IF NOT EXISTS ai_scheduled_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_slug VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tool_name VARCHAR(100) NOT NULL,
    tool_arguments JSONB,
    frequency VARCHAR(20) NOT NULL,
    cron_expression VARCHAR(100),
    next_execution_at TIMESTAMP,
    last_executed_at TIMESTAMP,
    last_result TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_scheduled_tasks_user_id ON ai_scheduled_tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_scheduled_tasks_status ON ai_scheduled_tasks(status, next_execution_at);
