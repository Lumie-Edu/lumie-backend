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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lumie.ai.application.dto.ChatReplyResponse;

import org.springframework.core.task.AsyncTaskExecutor;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT_SCHEMA_HEADER = "\nTenant schema (tables and columns):\n%s";

    private final AsyncTaskExecutor aiChatExecutor;
    private final ChatSystemPrompt chatSystemPrompt;

    private final ChatClient chatClient;
    private final LumieReadTools readTools;
    private final LumieWriteTools writeTools;
    private final ToolRegistry toolRegistry;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final SchemaDiscoveryService schemaDiscoveryService;

    // ── Streaming endpoint (SseEmitter: proper connection lifecycle) ──

    private static final int CHUNK_SIZE = 3;
    private static final long SSE_TIMEOUT = 120_000L; // 2 minutes

    public SseEmitter chatStreamEmitter(Long conversationId, String userMessage) {
        Long userId = UserContextHolder.getRequiredUserId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        // aiChatExecutor is a plain (non-Spring-decorated) executor, so the
        // request-thread tenant context does not propagate automatically.
        // Capture tenantId too — setTenant(slug) alone leaves RLS rows
        // invisible on the SSE thread's tenant-scoped writes (HARD_RULES).
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        aiChatExecutor.execute(() -> {
            try {
                TenantContextHolder.setTenant(tenantSlug);
                TenantContextHolder.setTenantId(tenantId);
                UserContextHolder.setUserId(userId);

                StreamContext ctx = prepareContext(conversationId, userMessage, userId, tenantSlug);

                ChatResponse response;
                try {
                    response = chatClient.prompt()
                            .system(ctx.systemPrompt)
                            .messages(ctx.history)
                            .tools(readTools, writeTools)
                            .call()
                            .chatResponse();
                } catch (PendingActionRequiredException e) {
                    sendPendingAction(emitter, ctx.conversation, e);
                    emitter.complete();
                    return;
                }

                logUsageIfPresent(response);
                String content = response.getResult().getOutput().getText();
                if (content == null) content = "";

                chatMessageRepository.save(ChatMessage.builder()
                        .conversationId(ctx.conversation.getId())
                        .role(MessageRole.ASSISTANT)
                        .content(content)
                        .build());

                // Emit content in small chunks to simulate streaming
                for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
                    String chunk = content.substring(i, Math.min(i + CHUNK_SIZE, content.length()));
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(objectMapper.writeValueAsString(Map.of("content", chunk))));
                    Thread.sleep(20);
                }

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(objectMapper.writeValueAsString(
                                Map.of("conversationId", ctx.conversation.getId()))));

                emitter.complete();

            } catch (Exception e) {
                log.error("Chat stream error: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"요청 처리 중 오류가 발생했습니다.\"}"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendPendingAction(SseEmitter emitter, Conversation conversation,
                                    PendingActionRequiredException e) throws Exception {
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

        emitter.send(SseEmitter.event()
                .name("pending")
                .data(objectMapper.writeValueAsString(Map.of(
                        "messageId", saved.getId(),
                        "toolName", e.getToolName(),
                        "description", e.getToolDescription(),
                        "arguments", e.getArguments()
                ))));
    }

    // ── Non-streaming endpoint (backward compat) ────────────────────────

    @Transactional
    public ChatReplyResponse chat(Long conversationId, String userMessage) {
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

            return ChatReplyResponse.of(ctx.conversation.getId(), content != null ? content : "");
        } catch (PendingActionRequiredException e) {
            return handlePendingActionSync(ctx.conversation, e);
        }
    }

    // ── Confirm endpoint ────────────────────────────────────────────────

    @Transactional
    public ChatReplyResponse confirm(Long messageId, boolean confirmed) {
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

            return ChatReplyResponse.of(conversation.getId(), "작업이 취소되었습니다.");
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
        String systemPrompt = chatSystemPrompt.prefix() + String.format(SYSTEM_PROMPT_SCHEMA_HEADER, schemaInfo);
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

            return ChatReplyResponse.of(conversation.getId(), content != null ? content : "");
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
        String systemPrompt = chatSystemPrompt.prefix() + String.format(SYSTEM_PROMPT_SCHEMA_HEADER, schemaInfo);
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

    private ChatReplyResponse handlePendingActionSync(Conversation conversation,
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

        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .content(buildConfirmationMessage(e.getToolName(), e.getArguments()))
                .pendingAction(pendingJson)
                .build());

        String pendingActionSummary;
        try {
            pendingActionSummary = objectMapper.writeValueAsString(Map.of(
                    "toolName", e.getToolName(),
                    "description", e.getToolDescription(),
                    "arguments", e.getArguments()
            ));
        } catch (JsonProcessingException ex) {
            pendingActionSummary = null;
        }

        return ChatReplyResponse.withPendingAction(
                conversation.getId(),
                buildConfirmationMessage(e.getToolName(), e.getArguments()),
                pendingActionSummary
        );
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
