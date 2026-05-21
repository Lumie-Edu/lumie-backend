package com.lumie.ai.adapter.in.web;

import com.lumie.ai.application.dto.response.ChatReplyResponse;
import com.lumie.ai.application.service.ChatService;
import com.lumie.common.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String CONFIRM_SCOPE = "POST /v1/chat/confirm";

    private final ChatService chatService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<ChatReplyResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request.conversationId(), request.message()));
    }

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter chatStream(
            @Valid @RequestBody ChatRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
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
                    () -> ResponseEntity.ok(chatService.confirm(request.messageId(), request.confirmed()))
            );
        }
        return ResponseEntity.ok(chatService.confirm(request.messageId(), request.confirmed()));
    }

    public record ChatRequest(
            Long conversationId,
            @NotBlank String message
    ) {
    }

    public record ConfirmRequest(
            Long messageId,
            boolean confirmed
    ) {
    }
}
