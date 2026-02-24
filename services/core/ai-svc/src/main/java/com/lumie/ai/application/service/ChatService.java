package com.lumie.ai.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.port.out.AiServicePort;
import com.lumie.ai.application.tool.Tool;
import com.lumie.ai.application.tool.ToolCategory;
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
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_TOOL_ITERATIONS = 10;

    private static final String SYSTEM_PROMPT = """
            You are an AI assistant for a Korean academy (학원) management system called Lumie.
            You help academy owners and administrators manage their business through natural language.

            You can:
            - Query student information, grades, attendance, and other data
            - Create announcements
            - Send notifications (SMS, Telegram)
            - Schedule recurring tasks

            When querying data, use the general_query tool with SQL. The database has these tables:
            - students (id, user_id, user_login_id, name, phone, academy_id, student_highschool, student_birth_year, student_memo, is_active)
            - admins (id, user_id, user_login_id, name, phone, is_active, admin_position, admin_memo)
            - academies (id, name, address, phone, is_active)
            - announcements (id, author_id, announcement_title, announcement_content, is_it_asset_announcement, is_it_important_announcement)
            - exams, exam_results (for test scores)
            - counseling_schedules, counseling_reservations
            - qna_boards, qna_comments
            - textbooks

            Always respond in Korean. Be concise and helpful.
            For write operations, clearly describe what you plan to do before executing.
            """;

    private final AiServicePort aiServicePort;
    private final ToolRegistry toolRegistry;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> chat(Long conversationId, String userMessage) {
        Long userId = UserContextHolder.getRequiredUserId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();

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

        List<Map<String, Object>> messages = buildMessageHistory(conversation.getId());
        List<Map<String, Object>> tools = toolRegistry.getAllToolSchemas();

        Map<String, Object> aiResponse = aiServicePort.chat(messages, tools);

        return processAiResponse(conversation, aiResponse, tenantSlug, 0);
    }

    @Transactional
    @SuppressWarnings("unchecked")
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

        List<Map<String, Object>> messages = buildMessageHistory(conversation.getId());
        List<Map<String, Object>> tools = toolRegistry.getAllToolSchemas();

        Map<String, Object> aiResponse = aiServicePort.chat(messages, tools);

        return processAiResponse(conversation, aiResponse, tenantSlug, 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processAiResponse(Conversation conversation,
                                                    Map<String, Object> aiResponse,
                                                    String tenantSlug,
                                                    int iteration) {
        if (iteration >= MAX_TOOL_ITERATIONS) {
            String fallback = "죄송합니다, 요청 처리 중 반복 제한에 도달했습니다.";
            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(conversation.getId())
                    .role(MessageRole.ASSISTANT)
                    .content(fallback)
                    .build());
            return Map.of("conversationId", conversation.getId(), "message", fallback);
        }

        String content = (String) aiResponse.get("content");
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) aiResponse.get("tool_calls");

        if (toolCalls == null || toolCalls.isEmpty()) {
            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(conversation.getId())
                    .role(MessageRole.ASSISTANT)
                    .content(content)
                    .build());

            return Map.of(
                    "conversationId", conversation.getId(),
                    "message", content
            );
        }

        String toolCallsJson;
        try {
            toolCallsJson = objectMapper.writeValueAsString(toolCalls);
        } catch (JsonProcessingException e) {
            toolCallsJson = null;
        }

        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .content(content)
                .toolCalls(toolCallsJson)
                .build());

        for (Map<String, Object> toolCall : toolCalls) {
            String toolCallId = (String) toolCall.get("id");
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String toolName = (String) function.get("name");
            String argsStr = (String) function.get("arguments");

            Map<String, Object> arguments;
            try {
                arguments = objectMapper.readValue(argsStr, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to parse tool arguments: {}", argsStr);
                arguments = Map.of();
            }

            Tool tool = toolRegistry.getTool(toolName).orElse(null);
            if (tool == null) {
                chatMessageRepository.save(ChatMessage.builder()
                        .conversationId(conversation.getId())
                        .role(MessageRole.TOOL)
                        .content("Unknown tool: " + toolName)
                        .toolCallId(toolCallId)
                        .build());
                continue;
            }

            if (tool.getCategory() == ToolCategory.WRITE || tool.getCategory() == ToolCategory.NOTIFICATION) {
                PendingAction pendingAction = new PendingAction(
                        toolCallId, toolName,
                        "실행 확인 필요: " + tool.getDescription(),
                        arguments
                );

                String pendingJson;
                try {
                    pendingJson = objectMapper.writeValueAsString(pendingAction);
                } catch (JsonProcessingException e) {
                    pendingJson = null;
                }

                ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                        .conversationId(conversation.getId())
                        .role(MessageRole.ASSISTANT)
                        .content(buildConfirmationMessage(toolName, arguments))
                        .pendingAction(pendingJson)
                        .build());

                return Map.of(
                        "conversationId", conversation.getId(),
                        "message", buildConfirmationMessage(toolName, arguments),
                        "pendingAction", Map.of(
                                "messageId", saved.getId(),
                                "toolName", toolName,
                                "description", pendingAction.description(),
                                "arguments", arguments
                        )
                );
            }

            ToolResult result = tool.execute(tenantSlug, arguments);

            chatMessageRepository.save(ChatMessage.builder()
                    .conversationId(conversation.getId())
                    .role(MessageRole.TOOL)
                    .content(result.success() ? result.data() : result.error())
                    .toolCallId(toolCallId)
                    .build());
        }

        List<Map<String, Object>> messages = buildMessageHistory(conversation.getId());
        List<Map<String, Object>> tools = toolRegistry.getAllToolSchemas();

        Map<String, Object> nextResponse = aiServicePort.chat(messages, tools);
        return processAiResponse(conversation, nextResponse, tenantSlug, iteration + 1);
    }

    private List<Map<String, Object>> buildMessageHistory(Long conversationId) {
        List<ChatMessage> history = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Map<String, Object>> messages = new ArrayList<>();

        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        for (ChatMessage msg : history) {
            // Skip pendingAction confirmation messages - they are UI-only and break
            // the DeepSeek message format (tool responses must follow assistant tool_calls)
            if (msg.getRole() == MessageRole.ASSISTANT && msg.getPendingAction() != null) {
                continue;
            }

            Map<String, Object> messageMap = new HashMap<>();

            switch (msg.getRole()) {
                case USER -> {
                    messageMap.put("role", "user");
                    messageMap.put("content", msg.getContent());
                }
                case ASSISTANT -> {
                    messageMap.put("role", "assistant");
                    if (msg.getContent() != null) {
                        messageMap.put("content", msg.getContent());
                    }
                    if (msg.getToolCalls() != null) {
                        try {
                            List<Map<String, Object>> toolCalls = objectMapper.readValue(
                                    msg.getToolCalls(), new TypeReference<>() {});
                            messageMap.put("tool_calls", toolCalls);
                        } catch (JsonProcessingException e) {
                            log.warn("Failed to parse tool_calls: {}", msg.getToolCalls());
                        }
                    }
                }
                case TOOL -> {
                    messageMap.put("role", "tool");
                    messageMap.put("content", msg.getContent());
                    if (msg.getToolCallId() != null) {
                        messageMap.put("tool_call_id", msg.getToolCallId());
                    }
                }
                case SYSTEM -> {
                    messageMap.put("role", "system");
                    messageMap.put("content", msg.getContent());
                }
            }

            messages.add(messageMap);
        }

        return messages;
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
}
