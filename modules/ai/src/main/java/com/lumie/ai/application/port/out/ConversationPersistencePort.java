package com.lumie.ai.application.port.out;

import com.lumie.ai.domain.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Application-owned persistence operations for conversations. */
public interface ConversationPersistencePort {

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    Page<Conversation> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    Conversation save(Conversation conversation);
}
