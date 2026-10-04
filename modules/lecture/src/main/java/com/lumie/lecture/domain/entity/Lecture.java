package com.lumie.lecture.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.lecture.domain.vo.LectureVisibility;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "lectures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private LectureVisibility visibility;

    @Column(name = "lecture_date")
    private LocalDate lectureDate;

    @Builder
    private Lecture(Long teacherId, String title,
                    String description, String youtubeUrl,
                    LectureVisibility visibility, LocalDate lectureDate) {
        this.teacherId = teacherId;
        this.title = title;
        this.description = description;
        this.youtubeUrl = youtubeUrl;
        this.visibility = visibility != null ? visibility : LectureVisibility.TEACHER_ONLY;
        this.lectureDate = lectureDate;
    }

    public static Lecture create(Long teacherId,
                                  String title, String description, String youtubeUrl,
                                  LectureVisibility visibility, LocalDate lectureDate) {
        return Lecture.builder()
                .teacherId(teacherId)
                .title(title)
                .description(description)
                .youtubeUrl(youtubeUrl)
                .visibility(visibility)
                .lectureDate(lectureDate)
                .build();
    }

    public void update(String title, String description, String youtubeUrl, LectureVisibility visibility,
                       LocalDate lectureDate) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (youtubeUrl != null) {
            this.youtubeUrl = youtubeUrl;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
        if (lectureDate != null) {
            this.lectureDate = lectureDate;
        }
    }

    public boolean isVisibleToStudents() {
        return this.visibility == LectureVisibility.STUDENT;
    }
}
