package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.application.port.out.ConversationPersistencePort;
import com.lumie.ai.domain.entity.Conversation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConversationPersistenceAdapter implements ConversationPersistencePort {

    private final ConversationJpaRepository repository;

    @Override
    public Optional<Conversation> findByIdAndUserId(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId);
    }

    @Override
    public Page<Conversation> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable) {
        return repository.findByUserIdAndIsActiveTrue(userId, pageable);
    }

    @Override
    public Conversation save(Conversation conversation) {
        return repository.save(conversation);
    }
}
