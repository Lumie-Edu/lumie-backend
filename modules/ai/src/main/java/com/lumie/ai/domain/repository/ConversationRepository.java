package com.lumie.ai.domain.repository;

import com.lumie.ai.domain.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    Page<Conversation> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);
}
