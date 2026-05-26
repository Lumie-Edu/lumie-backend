package com.lumie.ai.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.dto.response.ChatReplyResponse;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Proxy to the LangGraph chatbot worker (chatbot-svc) — the only chat path
 * after the Phase 5 Spring AI removal. BE → worker is network-trust +
 * {@code X-Tenant-Slug} (same as {@code ReportServiceClient}); the worker
 * calls back into {@code /internal/chatbot/**} (HMAC) for all
 * execution/persistence.
 *
 * <p>All methods run OUTSIDE any {@code @Transactional} boundary — the LLM call
 * lives entirely in the Python service, so the monolith never holds a DB
 * transaction open across the model round-trip.
 */
@Slf4j
@Component
public class ChatbotClient {

    private static final long SSE_TIMEOUT = 120_000L;

    private final RestClient restClient;
    private final AsyncTaskExecutor aiChatExecutor;
    private final ObjectMapper objectMapper;

    public ChatbotClient(RestClient.Builder restClientBuilder,
                         @Qualifier("aiChatExecutor") AsyncTaskExecutor aiChatExecutor,
                         ObjectMapper objectMapper,
                         @Value("${lumie.services.chatbot.url}") String chatbotUrl) {
        // Pin HTTP/1.1: the worker (uvicorn) is HTTP/1.1-only, but the default
        // JDK HttpClient negotiates HTTP/2 (h2c) over plaintext, which uvicorn
        // rejects ("Invalid HTTP request received") and the BE→worker call fails.
        HttpClient http11 = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.restClient = restClientBuilder
                .requestFactory(new JdkClientHttpRequestFactory(http11))
                .baseUrl(chatbotUrl)
                .build();
        this.aiChatExecutor = aiChatExecutor;
        this.objectMapper = objectMapper;
    }

    /** Proxy the worker's SSE stream straight through to the client emitter. */
    public SseEmitter streamChat(Long conversationId, String message,
                                 String tenantSlug, Long tenantId, Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(e -> cancelled.set(true));

        ChatStreamRequest body = new ChatStreamRequest(conversationId, message, tenantSlug, tenantId, userId);

        aiChatExecutor.execute(() -> {
            // aiChatExecutor is a plain virtual-thread executor with no TaskDecorator,
            // so re-establish the tenant/user context on this thread (HARD_RULES).
            TenantContextHolder.setTenant(tenantSlug);
            TenantContextHolder.setTenantId(tenantId);
            UserContextHolder.setUserId(userId);
            try {
                restClient.post()
                        .uri("/api/chatbot/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Slug", tenantSlug)
                        .body(body)
                        .exchange((req, res) -> {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
                                proxyFrames(reader, emitter, cancelled);
                            }
                            return null;
                        });
                safeComplete(emitter);
            } catch (Exception e) {
                log.error("chatbot-svc stream proxy failed for tenant {}: {}", tenantSlug, e.getMessage());
                safeCompleteWithError(emitter, e);
            } finally {
                TenantContextHolder.clear();
                UserContextHolder.clear();
            }
        });
        return emitter;
    }

    /** Non-streaming chat: consume the worker's stream and assemble a single reply. */
    public ChatReplyResponse chat(Long conversationId, String message,
                                  String tenantSlug, Long tenantId, Long userId) {
        ChatStreamRequest body = new ChatStreamRequest(conversationId, message, tenantSlug, tenantId, userId);
        StringBuilder content = new StringBuilder();
        AtomicReference<Long> resolvedConversationId = new AtomicReference<>(conversationId);
        AtomicReference<String> pendingAction = new AtomicReference<>();

        try {
            restClient.post()
                    .uri("/api/chatbot/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Slug", tenantSlug)
                    .body(body)
                    .exchange((req, res) -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
                            accumulateFrames(reader, content, resolvedConversationId, pendingAction);
                        }
                        return null;
                    });
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("chatbot-svc chat failed for tenant {}: {}", tenantSlug, e.getMessage());
            throw new AiException(AiErrorCode.INTERNAL_SERVICE_ERROR, "Chatbot service call failed");
        }

        if (pendingAction.get() != null) {
            return ChatReplyResponse.withPendingAction(
                    resolvedConversationId.get(), content.toString(), pendingAction.get());
        }
        return ChatReplyResponse.of(resolvedConversationId.get(), content.toString());
    }

    /** Resume an interrupted graph after the user confirms/rejects a pending action. */
    public ChatReplyResponse confirm(Long conversationId, Long messageId, boolean confirmed,
                                     String tenantSlug, Long tenantId, Long userId) {
        ConfirmRequest body = new ConfirmRequest(conversationId, messageId, confirmed, tenantSlug, tenantId, userId);
        try {
            JsonNode node = restClient.post()
                    .uri("/api/chatbot/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Slug", tenantSlug)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return toReply(node, conversationId);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("chatbot-svc confirm failed for tenant {}: {}", tenantSlug, e.getMessage());
            throw new AiException(AiErrorCode.INTERNAL_SERVICE_ERROR, "Chatbot confirm call failed");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Re-emit each SSE frame from the worker verbatim, preserving event names + data. */
    private void proxyFrames(BufferedReader reader, SseEmitter emitter, AtomicBoolean cancelled)
            throws IOException {
        String line;
        String eventName = null;
        StringBuilder data = new StringBuilder();
        while (!cancelled.get() && (line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (!relaySendFrame(emitter, eventName, data.toString())) {
                    cancelled.set(true); // client gone — stop reading, close worker stream
                    return;
                }
                eventName = null;
                data.setLength(0);
            } else if (line.startsWith("event:")) {
                eventName = line.substring(6).strip();
            } else if (line.startsWith("data:")) {
                if (data.length() > 0) {
                    data.append("\n");
                }
                data.append(line.substring(5).strip());
            }
        }
        // Flush a trailing frame if the worker closed without a final blank line.
        if (!cancelled.get()) {
            relaySendFrame(emitter, eventName, data.toString());
        }
    }

    /** @return false if the client has disconnected (send failed). */
    private boolean relaySendFrame(SseEmitter emitter, String eventName, String data) {
        if (eventName == null && data.isEmpty()) {
            return true;
        }
        try {
            SseEmitter.SseEventBuilder ev = SseEmitter.event();
            if (eventName != null) {
                ev.name(eventName);
            }
            ev.data(data);
            emitter.send(ev);
            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }

    /** Collapse the worker's stream into a final message + optional pending action. */
    private void accumulateFrames(BufferedReader reader, StringBuilder content,
                                  AtomicReference<Long> conversationId,
                                  AtomicReference<String> pendingAction) throws IOException {
        String line;
        String eventName = null;
        StringBuilder data = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                consumeFrame(eventName, data.toString(), content, conversationId, pendingAction);
                eventName = null;
                data.setLength(0);
            } else if (line.startsWith("event:")) {
                eventName = line.substring(6).strip();
            } else if (line.startsWith("data:")) {
                if (data.length() > 0) {
                    data.append("\n");
                }
                data.append(line.substring(5).strip());
            }
        }
        // Flush a trailing frame (worker closed without a final blank line).
        consumeFrame(eventName, data.toString(), content, conversationId, pendingAction);
    }

    private void consumeFrame(String eventName, String data, StringBuilder content,
                              AtomicReference<Long> conversationId,
                              AtomicReference<String> pendingAction) {
        if (eventName == null || data.isEmpty()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(data);
            switch (eventName) {
                case "token" -> content.append(node.path("content").asText(""));
                case "pending" -> pendingAction.set(data);
                case "done" -> {
                    if (node.hasNonNull("conversationId")) {
                        conversationId.set(node.get("conversationId").asLong());
                    }
                }
                case "error" -> throw new AiException(AiErrorCode.INTERNAL_SERVICE_ERROR,
                        node.path("message").asText("Chatbot error"));
                default -> { /* tool frames carry no reply content */ }
            }
        } catch (IOException e) {
            log.warn("Unparseable SSE frame from chatbot-svc ({}): {}", eventName, e.getMessage());
        }
    }

    private ChatReplyResponse toReply(JsonNode node, Long fallbackConversationId) {
        if (node == null) {
            return ChatReplyResponse.of(fallbackConversationId, "");
        }
        Long conversationId = node.hasNonNull("conversationId")
                ? node.get("conversationId").asLong() : fallbackConversationId;
        String message = node.path("message").asText("");
        String pendingAction = node.hasNonNull("pendingAction")
                ? node.get("pendingAction").toString() : null;
        return pendingAction != null
                ? ChatReplyResponse.withPendingAction(conversationId, message, pendingAction)
                : ChatReplyResponse.of(conversationId, message);
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // already completed (e.g. timeout fired) — nothing to do
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, Throwable e) {
        try {
            emitter.completeWithError(e);
        } catch (IllegalStateException ignored) {
            // already completed — nothing to do
        }
    }

    // ── request bodies (typed, camelCase matches the worker's Pydantic aliases) ──

    private record ChatStreamRequest(
            Long conversationId, String message, String tenantSlug, Long tenantId, Long userId) {}

    private record ConfirmRequest(
            Long conversationId, Long messageId, boolean confirmed,
            String tenantSlug, Long tenantId, Long userId) {}
}
