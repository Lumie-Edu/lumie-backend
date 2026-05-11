package com.lumie.content.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "announcement_title", nullable = false, length = 200)
    private String announcementTitle;

    @Column(name = "announcement_content", nullable = false, columnDefinition = "TEXT")
    private String announcementContent;

    @Column(name = "is_it_asset_announcement", nullable = false)
    private Boolean isItAssetAnnouncement;

    @Column(name = "is_it_important_announcement", nullable = false)
    private Boolean isItImportantAnnouncement;

    @Builder
    private Announcement(Long authorId, String announcementTitle, String announcementContent,
                         Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement) {
        this.authorId = authorId;
        this.announcementTitle = announcementTitle;
        this.announcementContent = announcementContent;
        this.isItAssetAnnouncement = isItAssetAnnouncement != null ? isItAssetAnnouncement : false;
        this.isItImportantAnnouncement = isItImportantAnnouncement != null ? isItImportantAnnouncement : false;
    }

    public static Announcement create(Long authorId, String announcementTitle, String announcementContent,
                                       Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement) {
        return Announcement.builder()
                .authorId(authorId)
                .announcementTitle(announcementTitle)
                .announcementContent(announcementContent)
                .isItAssetAnnouncement(isItAssetAnnouncement)
                .isItImportantAnnouncement(isItImportantAnnouncement)
                .build();
    }

    public void update(String announcementTitle, String announcementContent,
                       Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement) {
        if (announcementTitle != null && !announcementTitle.isBlank()) {
            this.announcementTitle = announcementTitle;
        }
        if (announcementContent != null && !announcementContent.isBlank()) {
            this.announcementContent = announcementContent;
        }
        if (isItAssetAnnouncement != null) {
            this.isItAssetAnnouncement = isItAssetAnnouncement;
        }
        if (isItImportantAnnouncement != null) {
            this.isItImportantAnnouncement = isItImportantAnnouncement;
        }
    }

    public void markImportant() {
        this.isItImportantAnnouncement = true;
    }

    public void unmarkImportant() {
        this.isItImportantAnnouncement = false;
    }
}
