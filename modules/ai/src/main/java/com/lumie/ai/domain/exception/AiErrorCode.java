package com.lumie.ai.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    CONVERSATION_NOT_FOUND("AI_001", "대화를 찾을 수 없습니다.", 404),
    MESSAGE_NOT_FOUND("AI_002", "메시지를 찾을 수 없습니다.", 404),
    SCHEDULED_TASK_NOT_FOUND("AI_003", "예약 작업을 찾을 수 없습니다.", 404),
    INVALID_SQL("AI_004", "요청한 작업을 처리할 수 없습니다.", 400),
    GEMINI_API_ERROR("AI_005", "AI 요청을 처리하지 못했습니다.", 502),
    PENDING_ACTION_NOT_FOUND("AI_006", "대기 중인 작업을 찾을 수 없습니다.", 404),
    TOOL_EXECUTION_ERROR("AI_007", "요청한 기능을 실행하지 못했습니다.", 500),
    INTERNAL_SERVICE_ERROR("AI_008", "요청을 처리하지 못했습니다.", 502),
    INVALID_TOOL("AI_009", "지원하지 않는 기능입니다.", 400);

    private final String code;
    private final String message;
    private final int status;
}
