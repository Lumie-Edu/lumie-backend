package com.lumie.ai.adapter.in.web;

import com.lumie.ai.application.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> chatStream(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")       // disable nginx/proxy buffering
                .header("Cache-Control", "no-cache, no-transform")
                .body(chatService.chatStream(request.conversationId(), request.message()));
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
