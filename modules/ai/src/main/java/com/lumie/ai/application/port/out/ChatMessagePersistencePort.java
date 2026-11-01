package com.lumie.ai.application.port.out;

import com.lumie.ai.domain.entity.ChatMessage;

import java.util.List;
import java.util.Optional;

/** Application-owned persistence operations for chat messages. */
public interface ChatMessagePersistencePort {

    Optional<ChatMessage> findById(Long id);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    ChatMessage save(ChatMessage message);
}
