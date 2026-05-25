package com.lumie.ai.adapter.in.web;

import com.lumie.ai.adapter.out.external.ChatbotClient;
import com.lumie.ai.application.dto.response.ChatReplyResponse;
import com.lumie.ai.application.service.ChatService;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private static final String CONFIRM_SCOPE = "POST /v1/chat/confirm";

    private final ChatService chatService;
    private final IdempotencyService idempotencyService;
    private final ChatbotClient chatbotClient;
    /** Strangler-fig switch: route chat to the LangGraph worker when enabled. */
    private final boolean langgraphEnabled;

    public ChatController(ChatService chatService,
                          IdempotencyService idempotencyService,
                          ChatbotClient chatbotClient,
                          @Value("${lumie.ai.langgraph.enabled:false}") boolean langgraphEnabled) {
        this.chatService = chatService;
        this.idempotencyService = idempotencyService;
        this.chatbotClient = chatbotClient;
        this.langgraphEnabled = langgraphEnabled;
    }

    @PostMapping
    public ResponseEntity<ChatReplyResponse> chat(@Valid @RequestBody ChatRequest request) {
        if (langgraphEnabled) {
            return ResponseEntity.ok(chatbotClient.chat(
                    request.conversationId(), request.message(),
                    TenantContextHolder.getRequiredTenant(),
                    TenantContextHolder.getRequiredTenantId(),
                    UserContextHolder.getRequiredUserId()));
        }
        return ResponseEntity.ok(chatService.chat(request.conversationId(), request.message()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @Valid @RequestBody ChatRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        if (langgraphEnabled) {
            return chatbotClient.streamChat(
                    request.conversationId(), request.message(),
                    TenantContextHolder.getRequiredTenant(),
                    TenantContextHolder.getRequiredTenantId(),
                    UserContextHolder.getRequiredUserId());
        }
        return chatService.chatStreamEmitter(request.conversationId(), request.message());
    }

    @PostMapping("/confirm")
    public ResponseEntity<ChatReplyResponse> confirm(
            @Valid @RequestBody ConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
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
        if (langgraphEnabled) {
            // The FE sends only messageId; the worker needs conversationId for the
            // graph thread id, so resolve it from the pending message first.
            Long conversationId = chatService.resolveConversationId(messageId);
            return chatbotClient.confirm(
                    conversationId, messageId, confirmed,
                    TenantContextHolder.getRequiredTenant(),
                    TenantContextHolder.getRequiredTenantId(),
                    UserContextHolder.getRequiredUserId());
        }
        return chatService.confirm(messageId, confirmed);
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
