package com.lumie.ai.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    CONVERSATION_NOT_FOUND("AI001", "Conversation not found", 404),
    MESSAGE_NOT_FOUND("AI002", "Message not found", 404),
    SCHEDULED_TASK_NOT_FOUND("AI003", "Scheduled task not found", 404),
    INVALID_SQL("AI004", "Invalid or unsafe SQL query", 400),
    DEEPSEEK_API_ERROR("AI005", "DeepSeek API error", 502),
    PENDING_ACTION_NOT_FOUND("AI006", "Pending action not found", 404),
    TOOL_EXECUTION_ERROR("AI007", "Tool execution failed", 500),
    INTERNAL_SERVICE_ERROR("AI008", "Internal service call failed", 502);

    private final String code;
    private final String message;
    private final int status;
}
