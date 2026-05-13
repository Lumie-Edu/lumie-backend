package com.lumie.ai.application.dto;

public record ChatReplyResponse(
        Long conversationId,
        String message,
        String pendingAction
) {
    public static ChatReplyResponse of(Long conversationId, String message) {
        return new ChatReplyResponse(conversationId, message, null);
    }

    public static ChatReplyResponse withPendingAction(Long conversationId, String message, String pendingAction) {
        return new ChatReplyResponse(conversationId, message, pendingAction);
    }
}
