package com.lumie.ai.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.tool.Tool;
import com.lumie.ai.application.tool.ToolRegistry;
import com.lumie.ai.application.tool.ToolResult;
import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.entity.Conversation;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.ai.domain.repository.ChatMessageRepository;
import com.lumie.ai.domain.repository.ConversationRepository;
import com.lumie.ai.domain.vo.MessageRole;
import com.lumie.ai.domain.vo.PendingAction;
import com.lumie.ai.infrastructure.ai.LumieReadTools;
import com.lumie.ai.infrastructure.ai.LumieWriteTools;
import com.lumie.ai.infrastructure.ai.PendingActionRequiredException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT_PREFIX = """
            You are Lumie AI, a Korean academy (학원) management assistant. Respond in Korean.

            EFFICIENCY: Answer every data question in exactly ONE generalQuery call. \
            Combine JOINs, subqueries, CASE, window functions, and aggregation into a single SQL. \
            Never split into multiple queries what can be done in one.

            RULES:
            - Never mention SQL, tables, columns, or any technical details to the user.
            - JSONB columns are queryable with ->, ->>, jsonb_array_elements, etc.
            - Always try a query before saying data is unavailable.
            - Be concise.

            Example — "블루밍 모의고사 8회 성적대별 학생 수":
            SELECT CASE WHEN er.total_score>=90 THEN '90점 이상' WHEN er.total_score>=80 THEN '80점대' \
            WHEN er.total_score>=70 THEN '70점대' WHEN er.total_score>=60 THEN '60점대' \
            ELSE '60점 미만' END AS band, COUNT(*) AS cnt \
            FROM exam_results er JOIN exams e ON er.exam_id=e.id \
            WHERE e.name='블루밍 모의고사 8회' GROUP BY 1 ORDER BY MIN(er.total_score) DESC

            """;

    private static final String SYSTEM_PROMPT_SCHEMA_HEADER = "\nTenant schema (tables and columns):\n%s";

    private final ChatClient chatClient;
    private final LumieReadTools readTools;
    private final LumieWriteTools writeTools;
    private final ToolRegistry toolRegistry;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final SchemaDiscoveryService schemaDiscoveryService;

    // ── Streaming endpoint (simulated: call() + chunked SSE emission) ──

    private static final int CHUNK_SIZE = 3; // characters per SSE token event

    public Flux<ServerSentEvent<String>> chatStream(Long conversationId, String userMessage) {
        Long userId = UserContextHolder.getRequiredUserId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        return Mono.fromCallable(() -> {
                    StreamContext ctx = prepareContext(conversationId, userMessage, userId, tenantSlug);

                    try {
                        ChatResponse response = chatClient.prompt()
                                .system(ctx.systemPrompt)
                                .messages(ctx.history)
                                .tools(readTools, writeTools)
                                .call()
                                .chatResponse();

                        logUsageIfPresent(response);
                        String content = response.getResult().getOutput().getText();
                        if (content == null) content = "";

                        chatMessageRepository.save(ChatMessage.builder()
                                .conversationId(ctx.conversation.getId())
                                .role(MessageRole.ASSISTANT)
                                .content(content)
                                .build());

                        return new StreamResult(ctx.conversation.getId(), content, null);
                    } catch (PendingActionRequiredException e) {
                        ServerSentEvent<String> pendingEvent = handlePendingAction(ctx.conversation, e);
                        return new StreamResult(ctx.conversation.getId(), null, pendingEvent);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(result -> {
                    if (result.pendingEvent != null) {
                        return Flux.just(result.pendingEvent);
                    }

                    // Emit content in small chunks to simulate streaming
                    List<ServerSentEvent<String>> events = new ArrayList<>();
                    String content = result.content;
                    for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
                        String chunk = content.substring(i, Math.min(i + CHUNK_SIZE, content.length()));
                        events.add(sseEvent("token", Map.of("content", chunk)));
                    }
                    events.add(sseEvent("done", Map.of("conversationId", result.conversationId)));

                    return Flux.fromIterable(events)
                            .delayElements(java.time.Duration.ofMillis(20));
                })
                .onErrorResume(e -> {
                    log.error("Chat stream error: {}", e.getMessage());
                    return Flux.just(sseEvent("error",
                            Map.of("message", "요청 처리 중 오류가 발생했습니다.")));
                });
    }

    private record StreamResult(Long conversationId, String content, ServerSentEvent<String> pendingEvent) {}

    // ── Non-streaming endpoint (backward compat) ────────────────────────

    @Transactional
    public Map<String, Object> chat(Long conversationId, String userMessage) {
        Long userId = UserContextHolder.getRequiredUserId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        StreamContext ctx = prepareContext(conversationId, userMessage, userId, tenantSlug);

        try {
            ChatResponse response = chatClient.prompt()
                    .system(ctx.systemPrompt)
                    .messages(ctx.history)
                    .tools(readTools, writeTools)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();

            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(ctx.conversation.getId())
                    .role(MessageRole.ASSISTANT)
                    .content(content)
                    .build());

            logUsageIfPresent(response);

            return Map.of(
                    "conversationId", ctx.conversation.getId(),
                    "message", content != null ? content : ""
            );
        } catch (PendingActionRequiredException e) {
            return handlePendingActionSync(ctx.conversation, e);
        }
    }

    // ── Confirm endpoint ────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> confirm(Long messageId, boolean confirmed) {
        Long userId = UserContextHolder.getRequiredUserId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        ChatMessage pendingMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AiException(AiErrorCode.MESSAGE_NOT_FOUND));

        if (pendingMessage.getPendingAction() == null) {
            throw new AiException(AiErrorCode.PENDING_ACTION_NOT_FOUND);
        }

        PendingAction action;
        try {
            action = objectMapper.readValue(pendingMessage.getPendingAction(), PendingAction.class);
        } catch (JsonProcessingException e) {
            throw new AiException(AiErrorCode.PENDING_ACTION_NOT_FOUND, "Failed to parse pending action");
        }

        Conversation conversation = conversationRepository
                .findByIdAndUserId(pendingMessage.getConversationId(), userId)
                .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));

        if (!confirmed) {
            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(conversation.getId())
                    .role(MessageRole.ASSISTANT)
                    .content("작업이 취소되었습니다.")
                    .build());

            return Map.of(
                    "conversationId", conversation.getId(),
                    "message", "작업이 취소되었습니다."
            );
        }

        Tool tool = toolRegistry.getTool(action.toolName())
                .orElseThrow(() -> new AiException(AiErrorCode.TOOL_EXECUTION_ERROR,
                        "Tool not found: " + action.toolName()));

        ToolResult result = tool.execute(tenantSlug, action.arguments());

        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.TOOL)
                .content(result.success() ? result.data() : result.error())
                .toolCallId(action.actionId())
                .build());

        String schemaInfo = schemaDiscoveryService.getSchemaDescription(tenantSlug);
        String systemPrompt = SYSTEM_PROMPT_PREFIX + String.format(SYSTEM_PROMPT_SCHEMA_HEADER, schemaInfo);
        List<Message> history = buildSpringAiHistory(conversation.getId());

        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(history)
                    .tools(readTools, writeTools)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();

            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(conversation.getId())
                    .role(MessageRole.ASSISTANT)
                    .content(content)
                    .build());

            return Map.of(
                    "conversationId", conversation.getId(),
                    "message", content != null ? content : ""
            );
        } catch (PendingActionRequiredException e) {
            return handlePendingActionSync(conversation, e);
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private StreamContext prepareContext(Long conversationId, String userMessage,
                                         Long userId, String tenantSlug) {
        // Restore ThreadLocal context — this method runs on boundedElastic threads
        // where the servlet-thread ThreadLocal values are not available.
        TenantContextHolder.setTenant(tenantSlug);
        UserContextHolder.setUserId(userId);

        Conversation conversation;
        if (conversationId != null) {
            conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));
        } else {
            conversation = conversationRepository.save(
                    Conversation.builder()
                            .userId(userId)
                            .title(userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage)
                            .build()
            );
        }

        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.USER)
                .content(userMessage)
                .build());

        String schemaInfo = schemaDiscoveryService.getSchemaDescription(tenantSlug);
        String systemPrompt = SYSTEM_PROMPT_PREFIX + String.format(SYSTEM_PROMPT_SCHEMA_HEADER, schemaInfo);
        List<Message> history = buildSpringAiHistory(conversation.getId());

        return new StreamContext(conversation, systemPrompt, history);
    }

    private List<Message> buildSpringAiHistory(Long conversationId) {
        List<ChatMessage> history = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Message> messages = new ArrayList<>();

        for (ChatMessage msg : history) {
            if (msg.getRole() == MessageRole.ASSISTANT && msg.getPendingAction() != null) {
                continue;
            }

            switch (msg.getRole()) {
                case USER -> messages.add(new UserMessage(msg.getContent()));
                case ASSISTANT -> messages.add(new AssistantMessage(
                        msg.getContent() != null ? msg.getContent() : ""));
                case TOOL -> messages.add(new UserMessage(
                        "[Tool result]: " + msg.getContent()));
                case SYSTEM -> messages.add(new SystemMessage(msg.getContent()));
            }
        }

        return messages;
    }

    private ServerSentEvent<String> handlePendingAction(Conversation conversation,
                                                         PendingActionRequiredException e) {
        PendingAction pendingAction = new PendingAction(
                UUID.randomUUID().toString(), e.getToolName(),
                e.getToolDescription(), e.getArguments());

        String pendingJson;
        try {
            pendingJson = objectMapper.writeValueAsString(pendingAction);
        } catch (JsonProcessingException ex) {
            pendingJson = null;
        }

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .content(buildConfirmationMessage(e.getToolName(), e.getArguments()))
                .pendingAction(pendingJson)
                .build());

        return sseEvent("pending", Map.of(
                "messageId", saved.getId(),
                "toolName", e.getToolName(),
                "description", e.getToolDescription(),
                "arguments", e.getArguments()
        ));
    }

    private Map<String, Object> handlePendingActionSync(Conversation conversation,
                                                         PendingActionRequiredException e) {
        PendingAction pendingAction = new PendingAction(
                UUID.randomUUID().toString(), e.getToolName(),
                e.getToolDescription(), e.getArguments());

        String pendingJson;
        try {
            pendingJson = objectMapper.writeValueAsString(pendingAction);
        } catch (JsonProcessingException ex) {
            pendingJson = null;
        }

        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .content(buildConfirmationMessage(e.getToolName(), e.getArguments()))
                .pendingAction(pendingJson)
                .build());

        return Map.of(
                "conversationId", conversation.getId(),
                "message", buildConfirmationMessage(e.getToolName(), e.getArguments()),
                "pendingAction", Map.of(
                        "messageId", saved.getId(),
                        "toolName", e.getToolName(),
                        "description", e.getToolDescription(),
                        "arguments", e.getArguments()
                )
        );
    }

    private String extractToken(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private void logUsageIfPresent(ChatResponse response) {
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return;
        }
        var usage = response.getMetadata().getUsage();
        if (usage.getTotalTokens() <= 0) {
            return;
        }
        try {
            MDC.put("promptTokens", String.valueOf(usage.getPromptTokens()));
            MDC.put("completionTokens", String.valueOf(usage.getCompletionTokens()));
            MDC.put("totalTokens", String.valueOf(usage.getTotalTokens()));
            log.info("AI usage recorded");
        } finally {
            MDC.remove("promptTokens");
            MDC.remove("completionTokens");
            MDC.remove("totalTokens");
        }
    }

    private ServerSentEvent<String> sseEvent(String eventType, Map<String, Object> data) {
        try {
            return ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data(objectMapper.writeValueAsString(data))
                    .build();
        } catch (JsonProcessingException e) {
            return ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data("{}")
                    .build();
        }
    }

    private String buildConfirmationMessage(String toolName, Map<String, Object> arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 작업을 실행하시겠습니까?\n\n");

        switch (toolName) {
            case "create_announcement" -> {
                sb.append("**공지사항 작성**\n");
                sb.append("- 제목: ").append(arguments.get("title")).append("\n");
                sb.append("- 내용: ").append(arguments.get("content")).append("\n");
            }
            case "send_sms" -> {
                sb.append("**SMS 발송**\n");
                sb.append("- 수신자: ").append(arguments.get("phoneNumber")).append("\n");
                sb.append("- 내용: ").append(arguments.get("message")).append("\n");
            }
            case "send_telegram" -> {
                sb.append("**텔레그램 메시지 발송**\n");
                sb.append("- 채팅 ID: ").append(arguments.get("chatId")).append("\n");
                sb.append("- 내용: ").append(arguments.get("message")).append("\n");
            }
            case "schedule_task" -> {
                sb.append("**작업 예약**\n");
                sb.append("- 이름: ").append(arguments.get("name")).append("\n");
                sb.append("- 주기: ").append(arguments.get("frequency")).append("\n");
            }
            default -> {
                sb.append("**").append(toolName).append("**\n");
                arguments.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
            }
        }

        sb.append("\n확인하시려면 승인, 취소하시려면 거부를 선택해주세요.");
        return sb.toString();
    }

    private record StreamContext(Conversation conversation, String systemPrompt, List<Message> history) {}
}
