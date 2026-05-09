package com.lumie.notification.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.notification.domain.vo.TemplateCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sms_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private TemplateCategory category;

    @Builder
    private SmsTemplate(String name, String content, TemplateCategory category) {
        this.name = name;
        this.content = content;
        this.category = category != null ? category : TemplateCategory.GENERAL;
    }

    public static SmsTemplate create(String name, String content, TemplateCategory category) {
        return SmsTemplate.builder()
                .name(name)
                .content(content)
                .category(category != null ? category : TemplateCategory.GENERAL)
                .build();
    }

    public void updateInfo(String name, String content, TemplateCategory category) {
        this.name = name;
        this.content = content;
        this.category = category;
    }
}
