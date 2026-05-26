package com.lumie.ai.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.entity.Conversation;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.ai.domain.repository.ChatMessageRepository;
import com.lumie.ai.domain.repository.ConversationRepository;
import com.lumie.ai.domain.vo.MessageRole;
import com.lumie.ai.domain.vo.PendingAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ChatService — persistence-only surface backing the LangGraph chatbot worker.
 *
 * <p>The in-monolith Spring AI chat path (Gemini via {@code ChatClient} +
 * {@code LumieRead/WriteTools}) was removed in the Phase 5 cutover. All chat
 * orchestration now lives in {@code chatbot-svc} (Python LangGraph); this
 * service exists solely so:
 *
 * <ul>
 *   <li>{@code ChatController} can resolve a pending message's
 *       {@code conversationId} on confirm (FE sends only {@code messageId}).</li>
 *   <li>{@code InternalChatbotController} can persist conversations / messages /
 *       pending actions on behalf of the worker. Every write method runs under
 *       {@code @Transactional} so {@code RlsTenantContextAspect} sets
 *       {@code app.tenant_id} on the insert connection — calling the
 *       repositories directly skips the aspect and the insert fails the RLS
 *       {@code WITH CHECK} policy.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    /**
     * Resolve the conversation a pending message belongs to. Used by the
     * confirm path (the FE only sends {@code messageId}, but the worker needs
     * {@code conversationId} to rebuild the graph thread id).
     */
    @Transactional(readOnly = true)
    public Long resolveConversationId(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .map(ChatMessage::getConversationId)
                .orElseThrow(() -> new AiException(AiErrorCode.MESSAGE_NOT_FOUND));
    }

    /** Windowed conversation history (last {@code limit} messages), RLS-scoped. */
    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentMessages(Long conversationId, int limit) {
        List<ChatMessage> all =
                chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int from = Math.max(0, all.size() - limit);
        return all.subList(from, all.size());
    }

    /** Create a conversation under RLS and return its id. */
    @Transactional
    public Long createConversation(Long userId, String title) {
        return conversationRepository.save(Conversation.builder()
                .userId(userId)
                .title(title)
                .build()).getId();
    }

    /** Persist a completed assistant/tool message under RLS and return its id. */
    @Transactional
    public Long saveInternalMessage(Long conversationId, MessageRole role, String content,
                                    String toolCallId) {
        return chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .toolCallId(toolCallId)
                .build()).getId();
    }

    /**
     * Persist a pending write action (LangGraph interrupt) under RLS and
     * return its id. {@code arguments} is intentionally open-schema
     * ({@code Map<String, Object>}) because the worker authors the tool
     * specs — strongly typing every possible tool's argument shape here
     * would couple the monolith to the LangGraph tool registry.
     */
    @Transactional
    public Long savePendingAction(Long conversationId, String toolName, String description,
                                  Map<String, Object> arguments) {
        PendingAction action = new PendingAction(
                UUID.randomUUID().toString(), toolName, description, arguments);
        String pendingJson;
        try {
            pendingJson = objectMapper.writeValueAsString(action);
        } catch (JsonProcessingException e) {
            throw new AiException(AiErrorCode.TOOL_EXECUTION_ERROR, "Failed to serialize pending action");
        }
        return chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .pendingAction(pendingJson)
                .build()).getId();
    }
}
