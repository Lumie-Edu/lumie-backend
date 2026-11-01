package com.lumie.ai.application.service;

import com.lumie.ai.application.port.out.ChatMessagePersistencePort;
import com.lumie.ai.application.port.out.ConversationPersistencePort;
import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.entity.Conversation;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.ai.domain.vo.MessageRole;
import com.lumie.common.tenant.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ConversationQueryServiceTest {

    @Mock
    private ConversationPersistencePort conversations;

    @Mock
    private ChatMessagePersistencePort messages;

    @InjectMocks
    private ConversationQueryService service;

    @BeforeEach
    void setUser() {
        UserContextHolder.setUserId(7L);
    }

    @AfterEach
    void clearUser() {
        UserContextHolder.clear();
    }

    @Test
    void listsOnlyTheCurrentUsersActiveConversationsWithPageMetadata() {
        var pageable = PageRequest.of(0, 2);
        Conversation first = conversation(10L, null);
        Conversation second = conversation(11L, "Science");
        given(conversations.findByUserIdAndIsActiveTrue(7L, pageable))
                .willReturn(new PageImpl<>(List.of(first, second), pageable, 3));

        var page = service.listConversations(pageable);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(response -> response.id())
                .containsExactly(10L, 11L);
        assertThat(page.getContent()).extracting(response -> response.title())
                .containsExactly("", "Science");
        verify(conversations).findByUserIdAndIsActiveTrue(7L, pageable);
    }

    @Test
    void getsOwnedConversationWithMessagesInPersistenceOrder() {
        Conversation conversation = conversation(10L, "Study");
        ChatMessage user = message(100L, MessageRole.USER, "Question", null);
        ChatMessage assistant = message(101L, MessageRole.ASSISTANT, "Answer", "{\"tool\":true}");
        given(conversations.findByIdAndUserId(10L, 7L)).willReturn(Optional.of(conversation));
        given(messages.findByConversationIdOrderByCreatedAtAsc(10L))
                .willReturn(List.of(user, assistant));

        var detail = service.getConversation(10L);

        assertThat(detail.id()).isEqualTo(10L);
        assertThat(detail.title()).isEqualTo("Study");
        assertThat(detail.isActive()).isTrue();
        assertThat(detail.messages()).extracting(response -> response.id())
                .containsExactly(100L, 101L);
        assertThat(detail.messages()).extracting(response -> response.role())
                .containsExactly("USER", "ASSISTANT");
        assertThat(detail.messages().get(1).pendingAction()).isEqualTo("{\"tool\":true}");
    }

    @Test
    void detailUsesEmptyTitleWhenConversationHasNoTitle() {
        given(conversations.findByIdAndUserId(10L, 7L))
                .willReturn(Optional.of(conversation(10L, null)));
        given(messages.findByConversationIdOrderByCreatedAtAsc(10L)).willReturn(List.of());

        var detail = service.getConversation(10L);

        assertThat(detail.title()).isEmpty();
        assertThat(detail.messages()).isEmpty();
    }

    @Test
    void missingOrForeignConversationFailsBeforeLoadingMessages() {
        given(conversations.findByIdAndUserId(10L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConversation(10L))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.CONVERSATION_NOT_FOUND));
        verifyNoInteractions(messages);
    }

    @Test
    void deleteDeactivatesOwnedConversationAndPersistsIt() {
        Conversation conversation = conversation(10L, "Study");
        given(conversations.findByIdAndUserId(10L, 7L)).willReturn(Optional.of(conversation));

        service.deleteConversation(10L);

        assertThat(conversation.isActive()).isFalse();
        verify(conversations).save(conversation);
    }

    @Test
    void deleteDoesNotMutateMissingOrForeignConversation() {
        given(conversations.findByIdAndUserId(10L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteConversation(10L))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.CONVERSATION_NOT_FOUND));
        verifyNoInteractions(messages);
    }

    private static Conversation conversation(Long id, String title) {
        Conversation conversation = Conversation.builder().userId(7L).title(title).build();
        ReflectionTestUtils.setField(conversation, "id", id);
        ReflectionTestUtils.setField(conversation, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(conversation, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));
        return conversation;
    }

    private static ChatMessage message(Long id, MessageRole role, String content, String pendingAction) {
        ChatMessage message = ChatMessage.builder()
                .conversationId(10L).role(role).content(content).pendingAction(pendingAction).build();
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        return message;
    }
}
