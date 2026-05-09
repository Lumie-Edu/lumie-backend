package com.lumie.ai.adapter.in.web;

import com.lumie.ai.application.service.ChatService;
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

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody ChatRequest request) {
        Map<String, Object> response = chatService.chat(request.conversationId(), request.message());
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> confirm(@Valid @RequestBody ConfirmRequest request) {
        Map<String, Object> response = chatService.confirm(request.messageId(), request.confirmed());
        return ResponseEntity.ok(response);
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
