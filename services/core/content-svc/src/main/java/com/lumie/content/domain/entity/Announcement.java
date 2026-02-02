package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "announcement_title", nullable = false, length = 200)
    private String announcementTitle;

    @Column(name = "announcement_content", nullable = false, columnDefinition = "TEXT")
    private String announcementContent;

    @Column(name = "is_it_asset_announcement", nullable = false)
    private Boolean isItAssetAnnouncement;

    @Column(name = "is_it_important_announcement", nullable = false)
    private Boolean isItImportantAnnouncement;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "announcement_academies",
        joinColumns = @JoinColumn(name = "announcement_id")
    )
    @Column(name = "academy_id")
    private Set<Long> academyIds = new HashSet<>();

    @Builder
    private Announcement(Long authorId, String announcementTitle, String announcementContent,
                         Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement,
                         Set<Long> academyIds) {
        this.authorId = authorId;
        this.announcementTitle = announcementTitle;
        this.announcementContent = announcementContent;
        this.isItAssetAnnouncement = isItAssetAnnouncement != null ? isItAssetAnnouncement : false;
        this.isItImportantAnnouncement = isItImportantAnnouncement != null ? isItImportantAnnouncement : false;
        this.academyIds = academyIds != null ? academyIds : new HashSet<>();
    }

    public static Announcement create(Long authorId, String announcementTitle, String announcementContent,
                                       Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement,
                                       Set<Long> academyIds) {
        return Announcement.builder()
                .authorId(authorId)
                .announcementTitle(announcementTitle)
                .announcementContent(announcementContent)
                .isItAssetAnnouncement(isItAssetAnnouncement)
                .isItImportantAnnouncement(isItImportantAnnouncement)
                .academyIds(academyIds)
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

    public void setAcademyIds(Set<Long> academyIds) {
        this.academyIds.clear();
        if (academyIds != null) {
            this.academyIds.addAll(academyIds);
        }
    }

    public void addAcademy(Long academyId) {
        this.academyIds.add(academyId);
    }

    public void removeAcademy(Long academyId) {
        this.academyIds.remove(academyId);
    }

    public void markImportant() {
        this.isItImportantAnnouncement = true;
    }

    public void unmarkImportant() {
        this.isItImportantAnnouncement = false;
    }
}
