package com.lumie.ai.domain.repository;

import com.lumie.ai.domain.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    Page<Conversation> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    void deleteById(Long id);
}
