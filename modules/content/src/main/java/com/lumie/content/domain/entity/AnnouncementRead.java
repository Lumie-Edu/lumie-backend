package com.lumie.content.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "announcement_reads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_announcement_reads", columnNames = {"announcement_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @Builder
    private AnnouncementRead(Announcement announcement, Long userId) {
        this.announcement = announcement;
        this.userId = userId;
        this.readAt = Instant.now();
    }

    public static AnnouncementRead create(Announcement announcement, Long userId) {
        return AnnouncementRead.builder()
                .announcement(announcement)
                .userId(userId)
                .build();
    }
}
