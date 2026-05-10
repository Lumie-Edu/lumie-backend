package com.lumie.ai.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    CONVERSATION_NOT_FOUND("AI_001", "Conversation not found", 404),
    MESSAGE_NOT_FOUND("AI_002", "Message not found", 404),
    SCHEDULED_TASK_NOT_FOUND("AI_003", "Scheduled task not found", 404),
    INVALID_SQL("AI_004", "Invalid or unsafe SQL query", 400),
    GEMINI_API_ERROR("AI_005", "Gemini API error", 502),
    PENDING_ACTION_NOT_FOUND("AI_006", "Pending action not found", 404),
    TOOL_EXECUTION_ERROR("AI_007", "Tool execution failed", 500),
    INTERNAL_SERVICE_ERROR("AI_008", "Internal service call failed", 502);

    private final String code;
    private final String message;
    private final int status;
}
