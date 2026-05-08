package com.lumie.homepage.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.homepage.domain.exception.HomepageNotReadyToPublishException;
import com.lumie.homepage.domain.vo.TemplateId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "homepage_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomepageConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_id", nullable = false, length = 64)
    private TemplateId templateId;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Column(name = "published", nullable = false)
    private boolean published;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections", nullable = false, columnDefinition = "jsonb")
    private String sections;

    @Builder
    private HomepageConfig(TemplateId templateId, int templateVersion, String sections) {
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.published = false;
        this.sections = sections;
    }

    public static HomepageConfig create(TemplateId templateId, int templateVersion, String sections) {
        return HomepageConfig.builder()
                .templateId(templateId)
                .templateVersion(templateVersion)
                .sections(sections)
                .build();
    }

    public void update(TemplateId templateId, int templateVersion, String sections) {
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.sections = sections;
    }

    /**
     * Marks this config as published.
     * Throws {@link HomepageNotReadyToPublishException} if {@code sections}
     * is null, empty, or a blank JSON object/array ({@code "{}"} / {@code "[]"}).
     */
    public void publish() {
        if (isBlankJson(this.sections)) {
            throw new HomepageNotReadyToPublishException();
        }
        this.published = true;
    }

    public void unpublish() {
        this.published = false;
    }

    private static boolean isBlankJson(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        return "{}".equals(trimmed) || "[]".equals(trimmed);
    }
}
