package com.lumie.ai.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public Conversation(Long userId, String title) {
        this.userId = userId;
        this.title = title;
        this.isActive = true;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
