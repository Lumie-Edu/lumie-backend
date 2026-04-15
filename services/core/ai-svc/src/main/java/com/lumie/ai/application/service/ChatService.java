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

    private static final int MAX_TOOL_ITERATIONS = 20;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are an AI assistant for a Korean academy (학원) management system called Lumie.
            You help academy owners and administrators manage their business through natural language.

            You can:
            - Query student information, grades, attendance, and other data
            - Create announcements
            - Send notifications (SMS, Telegram)
            - Schedule recurring tasks

            When querying data, use the general_query tool with SQL. The database has these tables:
            %s

            IMPORTANT: Be efficient with queries. Combine multiple questions into a single SQL query \
            using JOINs, subqueries, or UNION where possible. Avoid querying information_schema - use \
            the table/column names listed above directly. Aim to answer in as few queries as possible.

            CRITICAL - Response style rules:
            - Always respond in Korean. Be concise and helpful.
            - NEVER mention technical details to the user. No database names, table names, column names, \
              SQL queries, field names, data types, or any implementation details.
            - The user is a non-technical academy owner. Speak as a friendly assistant, not an engineer.
            - When something is not possible, explain simply what information is unavailable and suggest \
              practical alternatives. For example, instead of "student_birth_year 필드에 연도만 저장되어 있어서", \
              say "현재 학생 정보에 생년만 등록되어 있어 생일(월/일)은 확인이 어렵습니다."
            - When querying fails or returns no data, give a clear, human-friendly explanation.
            - For write operations, clearly describe what you plan to do before executing.
            """;

    private final AiServicePort aiServicePort;
    private final ToolRegistry toolRegistry;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final SchemaDiscoveryService schemaDiscoveryService;

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

        List<Map<String, Object>> messages = buildMessageHistory(conversation.getId(), tenantSlug);
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

        List<Map<String, Object>> messages = buildMessageHistory(conversation.getId(), tenantSlug);
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

        List<Map<String, Object>> messages = buildMessageHistory(conversation.getId(), tenantSlug);
        List<Map<String, Object>> tools = toolRegistry.getAllToolSchemas();

        Map<String, Object> nextResponse = aiServicePort.chat(messages, tools);
        return processAiResponse(conversation, nextResponse, tenantSlug, iteration + 1);
    }

    private List<Map<String, Object>> buildMessageHistory(Long conversationId, String tenantSlug) {
        List<ChatMessage> history = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        String schemaInfo = schemaDiscoveryService.getSchemaDescription(tenantSlug);
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, schemaInfo);

        List<Map<String, Object>> messages = new ArrayList<>();

        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (ChatMessage msg : history) {
            // Skip pendingAction confirmation messages - they are UI-only and break
            // the OpenAI-compatible message format (tool responses must follow assistant tool_calls)
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
