package com.lumie.ai.application.service;

import com.lumie.ai.application.dto.response.ConversationDetailResponse;
import com.lumie.ai.application.dto.response.ConversationSummaryResponse;
import com.lumie.ai.application.dto.response.MessageResponse;
import com.lumie.ai.application.port.out.ChatMessagePersistencePort;
import com.lumie.ai.application.port.out.ConversationPersistencePort;
import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.entity.Conversation;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationQueryService {

    private final ConversationPersistencePort conversationPersistencePort;
    private final ChatMessagePersistencePort chatMessagePersistencePort;

    public Page<ConversationSummaryResponse> listConversations(Pageable pageable) {
        Long userId = UserContextHolder.getRequiredUserId();
        return conversationPersistencePort.findByUserIdAndIsActiveTrue(userId, pageable)
                .map(this::toConversationSummary);
    }

    public ConversationDetailResponse getConversation(Long conversationId) {
        Long userId = UserContextHolder.getRequiredUserId();
        Conversation conversation = conversationPersistencePort.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));

        List<ChatMessage> messages = chatMessagePersistencePort
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<MessageResponse> messageList = messages.stream()
                .map(this::toMessageResponse)
                .toList();

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle() != null ? conversation.getTitle() : "",
                conversation.isActive(),
                messageList,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        Long userId = UserContextHolder.getRequiredUserId();
        Conversation conversation = conversationPersistencePort.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));
        conversation.deactivate();
        conversationPersistencePort.save(conversation);
    }

    private ConversationSummaryResponse toConversationSummary(Conversation conversation) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle() != null ? conversation.getTitle() : "",
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getPendingAction(),
                message.getCreatedAt()
        );
    }
}
