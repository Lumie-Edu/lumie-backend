package com.lumie.homepage.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.homepage.domain.exception.HomepageErrorCode;
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
public class HomepageConfig extends TenantScopedEntity {

    /** Current landing-template schema version. Must stay in sync with the FE
     *  {@code CURRENT_TEMPLATE_VERSION}. */
    public static final int CURRENT_TEMPLATE_VERSION = 3;

    /** Canonical empty homepage returned when a tenant has not saved one yet. */
    private static final String DEFAULT_SECTIONS =
            "{\"hero\":{\"enabled\":true,\"welcomeMessage\":\"\",\"kakaoChannelUrl\":\"\","
                    + "\"backgroundImageId\":null,\"backgroundImageUrl\":null}}";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

    /**
     * A transient (unsaved, {@code id == null}) default config for tenants that
     * have never customised their homepage. Returned by {@code GET /v1/homepage}
     * so the client always has a valid representation to render — never a 404.
     */
    public static HomepageConfig defaultConfig() {
        return HomepageConfig.builder()
                .templateId(TemplateId.SOLO_INSTRUCTOR)
                .templateVersion(CURRENT_TEMPLATE_VERSION)
                .sections(DEFAULT_SECTIONS)
                .build();
    }

    public void update(TemplateId templateId, int templateVersion, String sections) {
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.sections = sections;
    }

    /**
     * Marks this config as published.
     * Throws {@link BusinessException} with {@link HomepageErrorCode#NOT_READY_TO_PUBLISH} if {@code sections}
     * is null, empty, or a blank JSON object/array ({@code "{}"} / {@code "[]"}).
     */
    public void publish() {
        if (isBlankJson(this.sections)) {
            throw new BusinessException(HomepageErrorCode.NOT_READY_TO_PUBLISH);
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
