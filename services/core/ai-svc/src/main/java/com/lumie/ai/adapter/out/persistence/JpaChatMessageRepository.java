package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.repository.ChatMessageRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepository {

    @Override
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
