package com.lumie.ai.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.port.out.ChatMessagePersistencePort;
import com.lumie.ai.application.port.out.ConversationPersistencePort;
import com.lumie.ai.domain.entity.ChatMessage;
import com.lumie.ai.domain.entity.Conversation;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.ai.domain.vo.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationPersistencePort conversations;

    @Mock
    private ChatMessagePersistencePort messages;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(conversations, messages, objectMapper);
    }

    @Test
    void resolvesConversationFromOwnedMessageAndRejectsMissingMessage() {
        ChatMessage message = message(10L, "Question");
        given(messages.findById(42L)).willReturn(Optional.of(message));
        given(messages.findById(99L)).willReturn(Optional.empty());

        assertThat(service.resolveConversationId(42L)).isEqualTo(10L);
        assertThatThrownBy(() -> service.resolveConversationId(99L))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.MESSAGE_NOT_FOUND));
    }

    @Test
    void recentMessagesKeepTheFinalWindowInOriginalOrder() {
        ChatMessage first = message(10L, "one");
        ChatMessage second = message(10L, "two");
        ChatMessage third = message(10L, "three");
        given(messages.findByConversationIdOrderByCreatedAtAsc(10L))
                .willReturn(List.of(first, second, third));

        assertThat(service.getRecentMessages(10L, 2)).containsExactly(second, third);
        assertThat(service.getRecentMessages(10L, 10)).containsExactly(first, second, third);
        assertThat(service.getRecentMessages(10L, 0)).isEmpty();
    }

    @Test
    void conversationAndMessageWritesReturnPersistedIds() {
        given(conversations.save(any(Conversation.class))).willAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            ReflectionTestUtils.setField(conversation, "id", 21L);
            return conversation;
        });
        given(messages.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 22L);
            return message;
        });

        assertThat(service.createConversation(7L, "Study")).isEqualTo(21L);
        assertThat(service.saveInternalMessage(21L, MessageRole.TOOL, "result", "call-1"))
                .isEqualTo(22L);

        ArgumentCaptor<Conversation> conversation = ArgumentCaptor.forClass(Conversation.class);
        verify(conversations).save(conversation.capture());
        assertThat(conversation.getValue().getUserId()).isEqualTo(7L);
        assertThat(conversation.getValue().getTitle()).isEqualTo("Study");

        ArgumentCaptor<ChatMessage> savedMessage = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages).save(savedMessage.capture());
        assertThat(savedMessage.getValue().getConversationId()).isEqualTo(21L);
        assertThat(savedMessage.getValue().getRole()).isEqualTo(MessageRole.TOOL);
        assertThat(savedMessage.getValue().getContent()).isEqualTo("result");
        assertThat(savedMessage.getValue().getToolCallId()).isEqualTo("call-1");
    }

    @Test
    void pendingActionPersistsTypedPayloadWithoutReplyContent() throws Exception {
        given(messages.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 31L);
            return message;
        });

        assertThat(service.savePendingAction(21L, "create_announcement", "Publish",
                Map.of("title", "Science"))).isEqualTo(31L);

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages).save(saved.capture());
        ChatMessage message = saved.getValue();
        assertThat(message.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(message.getContent()).isNull();
        var payload = objectMapper.readTree(message.getPendingAction());
        assertThat(payload.path("actionId").asText()).isNotBlank();
        assertThat(payload.path("toolName").asText()).isEqualTo("create_announcement");
        assertThat(payload.path("description").asText()).isEqualTo("Publish");
        assertThat(payload.path("arguments").path("title").asText()).isEqualTo("Science");
    }

    @Test
    void serializationFailureDoesNotPersistPendingAction() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        given(failingMapper.writeValueAsString(any()))
                .willThrow(new JsonProcessingException("serialization failed") {});
        ChatService failingService = new ChatService(conversations, messages, failingMapper);

        assertThatThrownBy(() -> failingService.savePendingAction(21L, "publish", "Publish", Map.of()))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.TOOL_EXECUTION_ERROR));
        verifyNoInteractions(messages);
    }

    private static ChatMessage message(Long conversationId, String content) {
        return ChatMessage.builder()
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }
}
