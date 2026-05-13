package com.lumie.ai.application.service;

import com.lumie.ai.application.dto.ConversationDetailResponse;
import com.lumie.ai.application.dto.ConversationSummaryResponse;
import com.lumie.ai.application.dto.MessageResponse;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationQueryService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public Page<ConversationSummaryResponse> listConversations(Pageable pageable) {
        Long userId = UserContextHolder.getRequiredUserId();
        return conversationRepository.findByUserIdAndIsActiveTrue(userId, pageable)
                .map(this::toConversationSummary);
    }

    public ConversationDetailResponse getConversation(Long conversationId) {
        Long userId = UserContextHolder.getRequiredUserId();
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));

        List<ChatMessage> messages = chatMessageRepository
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
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.CONVERSATION_NOT_FOUND));
        conversation.deactivate();
        conversationRepository.save(conversation);
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
