package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.domain.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data queries for chat message storage. */
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
