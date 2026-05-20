package com.lumie.ai.adapter.in.web;

import com.lumie.ai.application.dto.response.ChatReplyResponse;
import com.lumie.ai.application.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

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
    public ResponseEntity<ChatReplyResponse> confirm(@Valid @RequestBody ConfirmRequest request) {
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
