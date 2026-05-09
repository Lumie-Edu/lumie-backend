package com.lumie.ai.domain.repository;

import com.lumie.ai.domain.entity.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<ChatMessage> findById(Long id);
}
