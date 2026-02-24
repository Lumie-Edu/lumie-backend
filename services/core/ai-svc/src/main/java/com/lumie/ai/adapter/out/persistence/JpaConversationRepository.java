package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.domain.entity.Conversation;
import com.lumie.ai.domain.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaConversationRepository extends JpaRepository<Conversation, Long>, ConversationRepository {

    @Override
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    @Override
    Page<Conversation> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);
}
