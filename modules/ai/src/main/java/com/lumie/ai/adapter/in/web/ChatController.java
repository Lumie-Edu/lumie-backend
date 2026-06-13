package com.lumie.ai.adapter.in.web;

import com.lumie.ai.adapter.out.external.ChatbotClient;
import com.lumie.ai.application.dto.response.ChatReplyResponse;
import com.lumie.ai.application.service.ChatService;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Public chat endpoints. Always proxies to the LangGraph chatbot worker
 * (chatbot-svc) — the Phase 5 cutover removed the in-monolith Spring AI
 * fallback. {@link ChatService} is retained for the confirm path
 * ({@code resolveConversationId}) because the FE sends only {@code messageId}
 * and the worker needs {@code conversationId} for the graph thread id.
 */
@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String CONFIRM_SCOPE = "POST /v1/chat/confirm";

    private final ChatService chatService;
    private final IdempotencyService idempotencyService;
    private final ChatbotClient chatbotClient;

    @PostMapping
    public ResponseEntity<ChatReplyResponse> chat(@Valid @RequestBody ChatRequest request) {
        requireStaffUser();
        return ResponseEntity.ok(chatbotClient.chat(
                request.conversationId(), request.message(),
                TenantContextHolder.getRequiredTenant(),
                TenantContextHolder.getRequiredTenantId(),
                UserContextHolder.getRequiredUserId()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @Valid @RequestBody ChatRequest request,
            HttpServletResponse response) {
        requireStaffUser();
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        return chatbotClient.streamChat(
                request.conversationId(), request.message(),
                TenantContextHolder.getRequiredTenant(),
                TenantContextHolder.getRequiredTenantId(),
                UserContextHolder.getRequiredUserId());
    }

    @PostMapping("/confirm")
    public ResponseEntity<ChatReplyResponse> confirm(
            @Valid @RequestBody ConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        requireStaffUser();
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    CONFIRM_SCOPE,
                    Map.of("messageId", request.messageId()),
                    () -> ResponseEntity.ok(doConfirm(request.messageId(), request.confirmed()))
            );
        }
        return ResponseEntity.ok(doConfirm(request.messageId(), request.confirmed()));
    }

    private ChatReplyResponse doConfirm(Long messageId, boolean confirmed) {
        // FE sends only messageId; the worker needs conversationId for the
        // graph thread id, so resolve it from the pending message first.
        Long conversationId = chatService.resolveConversationId(messageId);
        return chatbotClient.confirm(
                conversationId, messageId, confirmed,
                TenantContextHolder.getRequiredTenant(),
                TenantContextHolder.getRequiredTenantId(),
                UserContextHolder.getRequiredUserId());
    }

    private void requireStaffUser() {
        Role role = UserContextHolder.getUserRole();
        if (role == null || role == Role.STUDENT) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    public record ChatRequest(
            Long conversationId,
            @NotBlank String message
    ) {
    }

    public record ConfirmRequest(
            @NotNull Long messageId,
            boolean confirmed
    ) {
    }
}
