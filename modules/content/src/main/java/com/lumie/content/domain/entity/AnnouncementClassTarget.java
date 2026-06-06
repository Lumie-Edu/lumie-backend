package com.lumie.content.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_class_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"announcement_id", "class_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementClassTarget extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Builder
    private AnnouncementClassTarget(Long announcementId, Long classId) {
        this.announcementId = announcementId;
        this.classId = classId;
    }

    public static AnnouncementClassTarget create(Long announcementId, Long classId) {
        return AnnouncementClassTarget.builder()
                .announcementId(announcementId)
                .classId(classId)
                .build();
    }
}
