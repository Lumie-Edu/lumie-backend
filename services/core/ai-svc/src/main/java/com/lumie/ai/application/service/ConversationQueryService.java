package com.lumie.ai.application.service;

import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.entity.Conversation;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.ai.domain.repository.ChatMessageRepository;
import com.lumie.ai.domain.repository.ConversationRepository;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationQueryService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public Page<Map<String, Object>> listConversations(Pageable pageable) {
        Long userId = UserContextHolder.getRequiredUserId();
        return conversationRepository.findByUserIdAndIsActiveTrue(userId, pageable)
                .map(this::toConversationSummary);
    }

    public Map<String, Object> getConversation(Long conversationId) {
        Long userId = UserContextHolder.getRequiredUserId();
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));

        List<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Map<String, Object>> messageList = messages.stream()
                .map(this::toMessageResponse)
                .toList();

        return Map.of(
                "id", conversation.getId(),
                "title", conversation.getTitle() != null ? conversation.getTitle() : "",
                "isActive", conversation.isActive(),
                "messages", messageList,
                "createdAt", conversation.getCreatedAt(),
                "updatedAt", conversation.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        Long userId = UserContextHolder.getRequiredUserId();
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));
        conversation.deactivate();
        conversationRepository.save(conversation);
    }

    private Map<String, Object> toConversationSummary(Conversation conversation) {
        return Map.of(
                "id", conversation.getId(),
                "title", conversation.getTitle() != null ? conversation.getTitle() : "",
                "createdAt", conversation.getCreatedAt(),
                "updatedAt", conversation.getUpdatedAt()
        );
    }

    private Map<String, Object> toMessageResponse(ChatMessage message) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", message.getId());
        map.put("role", message.getRole().name());
        map.put("content", message.getContent());
        map.put("createdAt", message.getCreatedAt());

        if (message.getPendingAction() != null) {
            map.put("pendingAction", message.getPendingAction());
        }

        return map;
    }
}
