package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.domain.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data queries for conversation storage. */
public interface ConversationJpaRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    Page<Conversation> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);
}
