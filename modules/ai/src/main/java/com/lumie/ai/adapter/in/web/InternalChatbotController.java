package com.lumie.ai.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.service.SchemaDiscoveryService;
import com.lumie.ai.application.tool.Tool;
import com.lumie.ai.application.tool.ToolRegistry;
import com.lumie.ai.application.tool.ToolResult;
import com.lumie.ai.application.tool.impl.GeneralQueryTool;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Internal callback surface for the LangGraph chatbot worker (chatbot-svc).
 *
 * <p>The Python service generates SQL / decides tool calls, but ALL execution and
 * persistence happens here so that the battle-tested controls stay in the monolith:
 * SqlValidator+RLS for reads, the existing {@link Tool} implementations for writes,
 * and RLS tenant isolation for every DB touch. The Python worker never connects to
 * tenant data tables.
 *
 * <p>Secured by {@code InternalHmacAuthFilter} → {@code ROLE_INTERNAL} (SecurityConfig
 * maps {@code /internal/**}). Each method re-establishes the tenant/user context
 * explicitly via {@link TenantContextHolder#withinContext} from the request body, so
 * RLS fires correctly regardless of header propagation.
 */
@Slf4j
@RestController
@RequestMapping("/internal/chatbot")
@RequiredArgsConstructor
public class InternalChatbotController {

    /** Write tools the worker is allowed to invoke after user confirmation. */
    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "create_announcement", "send_sms", "send_telegram", "schedule_task");

    private final GeneralQueryTool generalQueryTool;
    private final ToolRegistry toolRegistry;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    /** Execute a read-only SQL query (SqlValidator + RLS + read-only pool). */
    @PostMapping("/query")
    public ResponseEntity<ToolResultResponse> query(@RequestBody QueryRequest req) {
        ToolResult result = TenantContextHolder.withinContext(
                req.tenantSlug(), req.tenantId(),
                () -> generalQueryTool.execute(req.tenantSlug(), Map.of("sql", req.sql())));
        return ResponseEntity.ok(ToolResultResponse.from(result));
    }

    /** Execute a confirmed write tool by name (allowlisted). */
    @PostMapping("/tools/{toolName}")
    public ResponseEntity<ToolResultResponse> executeTool(
            @PathVariable String toolName, @RequestBody ToolExecuteRequest req) {
        if (!ALLOWED_TOOLS.contains(toolName)) {
            throw new AiException(AiErrorCode.INVALID_TOOL, "Tool not permitted: " + toolName);
        }
        Tool tool = toolRegistry.getTool(toolName)
                .orElseThrow(() -> new AiException(AiErrorCode.INVALID_TOOL, "Unknown tool: " + toolName));

        ToolResult result = TenantContextHolder.withinContext(
                req.tenantSlug(), req.tenantId(),
                () -> {
                    // Save/restore userId explicitly — withinContext only manages the
                    // tenant ThreadLocals, not UserContextHolder.
                    Long previousUserId = UserContextHolder.getUserId();
                    UserContextHolder.setUserId(req.userId());
                    try {
                        return tool.execute(req.tenantSlug(), req.arguments());
                    } finally {
                        UserContextHolder.setUserId(previousUserId);
                    }
                });
        return ResponseEntity.ok(ToolResultResponse.from(result));
    }

    /** Tenant DB schema description injected into the agent's planning context. */
    @GetMapping("/schema")
    public ResponseEntity<SchemaResponse> schema(@RequestParam String tenantSlug) {
        return ResponseEntity.ok(new SchemaResponse(schemaDiscoveryService.getSchemaDescription(tenantSlug)));
    }

    /** Windowed conversation history (last {@code limit} messages). */
    @GetMapping("/history")
    public ResponseEntity<HistoryResponse> history(
            @RequestParam String tenantSlug,
            @RequestParam Long tenantId,
            @RequestParam Long conversationId,
            @RequestParam(defaultValue = "20") int limit) {
        List<MessageDto> messages = TenantContextHolder.withinContext(tenantSlug, tenantId, () -> {
            List<ChatMessage> all = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            int from = Math.max(0, all.size() - limit);
            return all.subList(from, all.size()).stream().map(MessageDto::from).toList();
        });
        return ResponseEntity.ok(new HistoryResponse(messages));
    }

    /** Persist a pending write action (LangGraph interrupt) and return its message id. */
    @PostMapping("/pending-action")
    public ResponseEntity<MessageIdResponse> savePendingAction(@RequestBody SavePendingActionRequest req) {
        Long messageId = TenantContextHolder.withinContext(req.tenantSlug(), req.tenantId(), () -> {
            PendingAction action = new PendingAction(
                    UUID.randomUUID().toString(), req.toolName(), req.description(), req.arguments());
            ChatMessage message = ChatMessage.builder()
                    .conversationId(req.conversationId())
                    .role(MessageRole.ASSISTANT)
                    .pendingAction(writeJson(action))
                    .build();
            return chatMessageRepository.save(message).getId();
        });
        return ResponseEntity.ok(new MessageIdResponse(messageId));
    }

    /** Persist a completed assistant/tool message and return its id. */
    @PostMapping("/save-message")
    public ResponseEntity<MessageIdResponse> saveMessage(@RequestBody SaveMessageRequest req) {
        MessageRole role = parseRole(req.role());
        Long messageId = TenantContextHolder.withinContext(req.tenantSlug(), req.tenantId(), () -> {
            ChatMessage message = ChatMessage.builder()
                    .conversationId(req.conversationId())
                    .role(role)
                    .content(req.content())
                    .toolCallId(req.toolCallId())
                    .build();
            return chatMessageRepository.save(message).getId();
        });
        return ResponseEntity.ok(new MessageIdResponse(messageId));
    }

    /** Create a new conversation and return its id. */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationIdResponse> createConversation(@RequestBody CreateConversationRequest req) {
        Long conversationId = TenantContextHolder.withinContext(req.tenantSlug(), req.tenantId(), () -> {
            Conversation conversation = Conversation.builder()
                    .userId(req.userId())
                    .title(req.title())
                    .build();
            return conversationRepository.save(conversation).getId();
        });
        return ResponseEntity.ok(new ConversationIdResponse(conversationId));
    }

    private MessageRole parseRole(String role) {
        try {
            return MessageRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new AiException(AiErrorCode.INVALID_TOOL, "Unknown message role: " + role);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new AiException(AiErrorCode.TOOL_EXECUTION_ERROR, "Failed to serialize pending action");
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public record QueryRequest(String tenantSlug, Long tenantId, Long userId, String sql) {}

    public record ToolExecuteRequest(
            String tenantSlug, Long tenantId, Long userId, Map<String, Object> arguments) {}

    public record ToolResultResponse(boolean success, String data, String error) {
        static ToolResultResponse from(ToolResult r) {
            return new ToolResultResponse(r.success(), r.data(), r.error());
        }
    }

    public record SchemaResponse(String description) {}

    public record HistoryResponse(List<MessageDto> messages) {}

    public record MessageDto(
            Long id, String role, String content, String toolCallId,
            String toolCalls, String pendingAction) {
        static MessageDto from(ChatMessage m) {
            return new MessageDto(
                    m.getId(), m.getRole().name(), m.getContent(),
                    m.getToolCallId(), m.getToolCalls(), m.getPendingAction());
        }
    }

    public record SavePendingActionRequest(
            String tenantSlug, Long tenantId, Long userId, Long conversationId,
            String toolName, String description, Map<String, Object> arguments) {}

    public record SaveMessageRequest(
            String tenantSlug, Long tenantId, Long conversationId,
            String role, String content, String toolCallId) {}

    public record CreateConversationRequest(
            String tenantSlug, Long tenantId, Long userId, String title) {}

    public record MessageIdResponse(Long messageId) {}

    public record ConversationIdResponse(Long conversationId) {}
}
