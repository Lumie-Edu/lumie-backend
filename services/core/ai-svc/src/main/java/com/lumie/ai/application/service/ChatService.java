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
            You are an AI assistant for a Korean academy (학원) management system called Lumie.
            You help academy owners and administrators manage their business through natural language.

            You can:
            - Query student information, grades, attendance, and other data
            - Create announcements
            - Send notifications (SMS, Telegram)
            - Schedule recurring tasks

            When querying data, use the generalQuery tool with SQL. Keep these rules in mind:

            IMPORTANT: Be efficient with queries. Combine multiple questions into a single SQL query \
            using JOINs, subqueries, or UNION where possible. Avoid querying information_schema - use \
            the table/column names listed in the schema section below directly. Aim to answer in as \
            few queries as possible.

            CRITICAL - Always attempt queries:
            - Never refuse a request because analysis "seems complex". PostgreSQL is capable; write the SQL and try.
            - JSONB columns (marked "JSONB" in the schema) are fully queryable with ->, ->>, jsonb_array_elements, \
              jsonb_each, jsonb_path_query, etc. Aggregate, filter, and join on JSONB fields freely.
            - Do NOT tell the user "I can't do this analysis" or "this is beyond my capability" before \
              executing at least one query. Try first; if the query fails or returns insufficient data, \
              then explain what is missing.
            - When a question requires multi-step analysis (e.g., per-category score breakdown from \
              question_results), generate the SQL with GROUP BY / aggregation / JSONB unnesting and call \
              the generalQuery tool. Don't describe what you would do - do it.

            CRITICAL - Response style rules:
            - Always respond in Korean. Be concise and helpful.
            - NEVER mention technical details to the user. No database names, table names, column names, \
              SQL queries, field names, data types, or any implementation details.
            - The user is a non-technical academy owner. Speak as a friendly assistant, not an engineer.
            - When something is not possible, explain simply what information is unavailable and suggest \
              practical alternatives.
            - When querying fails or returns no data, give a clear, human-friendly explanation.
            - For write operations, clearly describe what you plan to do before executing.

            SQL examples (adapt column names to the actual schema below):

            Q: "김민수 학생의 최근 시험 점수 보여줘"
            SQL: SELECT e.name, er.total_score, er.grade
                 FROM exam_results er
                 JOIN exams e ON er.exam_id = e.id
                 JOIN students s ON er.student_id = s.id
                 WHERE s.name = '김민수'
                 ORDER BY er.created_at DESC
                 LIMIT 5

            Q: "블루밍 모의고사 8회의 성적대별 학생 수"
            SQL: SELECT CASE
                          WHEN er.total_score >= 90 THEN '90점 이상'
                          WHEN er.total_score >= 80 THEN '80점대'
                          WHEN er.total_score >= 70 THEN '70점대'
                          WHEN er.total_score >= 60 THEN '60점대'
                          ELSE '60점 미만'
                        END AS band,
                        COUNT(*) AS student_count
                 FROM exam_results er
                 JOIN exams e ON er.exam_id = e.id
                 WHERE e.name = '블루밍 모의고사 8회'
                 GROUP BY 1
                 ORDER BY MIN(er.total_score) DESC

            Q: "이번 달 출결이 안 좋은 학생 상위 5명"
            SQL: SELECT s.name, COUNT(*) FILTER (WHERE a.status <> 'PRESENT') AS absent_count
                 FROM students s
                 JOIN attendances a ON a.student_id = s.id
                 WHERE a.attended_at >= date_trunc('month', CURRENT_DATE)
                 GROUP BY s.id, s.name
                 HAVING COUNT(*) FILTER (WHERE a.status <> 'PRESENT') > 0
                 ORDER BY absent_count DESC
                 LIMIT 5

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

    // ── Streaming endpoint ──────────────────────────────────────────────

    public Flux<ServerSentEvent<String>> chatStream(Long conversationId, String userMessage) {
        Long userId = UserContextHolder.getRequiredUserId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        return Mono.fromCallable(() -> prepareContext(conversationId, userMessage, userId, tenantSlug))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(ctx -> {
                    StringBuilder fullContent = new StringBuilder();

                    return chatClient.prompt()
                            .system(ctx.systemPrompt)
                            .messages(ctx.history)
                            .tools(readTools, writeTools)
                            .stream()
                            .chatResponse()
                            .mapNotNull(response -> {
                                String token = extractToken(response);
                                if (token != null && !token.isEmpty()) {
                                    fullContent.append(token);
                                    return sseEvent("token", Map.of("content", token));
                                }
                                logUsageIfPresent(response);
                                return null;
                            })
                            .onErrorResume(PendingActionRequiredException.class, e ->
                                    Mono.fromCallable(() -> handlePendingAction(ctx.conversation, e))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .flux())
                            .concatWith(Mono.fromCallable(() -> {
                                if (!fullContent.isEmpty()) {
                                    chatMessageRepository.save(ChatMessage.builder()
                                            .conversationId(ctx.conversation.getId())
                                            .role(MessageRole.ASSISTANT)
                                            .content(fullContent.toString())
                                            .build());
                                }
                                return sseEvent("done", Map.of(
                                        "conversationId", ctx.conversation.getId()));
                            }).subscribeOn(Schedulers.boundedElastic()))
                            .onErrorResume(e -> {
                                log.error("Chat stream error: {}", e.getMessage());
                                return Flux.just(sseEvent("error",
                                        Map.of("message", "요청 처리 중 오류가 발생했습니다.")));
                            });
                });
    }

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
