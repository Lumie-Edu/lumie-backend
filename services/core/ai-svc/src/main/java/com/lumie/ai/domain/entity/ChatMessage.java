package com.lumie.ai.domain.entity;

import com.lumie.ai.domain.vo.MessageRole;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_call_id", length = 100)
    private String toolCallId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private String toolCalls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pending_action", columnDefinition = "jsonb")
    private String pendingAction;

    @Builder
    public ChatMessage(Long conversationId, MessageRole role, String content,
                       String toolCallId, String toolCalls, String pendingAction) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolCalls = toolCalls;
        this.pendingAction = pendingAction;
    }
}
