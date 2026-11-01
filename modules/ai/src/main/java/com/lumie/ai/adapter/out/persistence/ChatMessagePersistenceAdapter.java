package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.application.port.out.ChatMessagePersistencePort;
import com.lumie.ai.domain.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatMessagePersistenceAdapter implements ChatMessagePersistencePort {

    private final ChatMessageJpaRepository repository;

    @Override
    public Optional<ChatMessage> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId) {
        return repository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        return repository.save(message);
    }
}
