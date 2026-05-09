package com.lumie.lecture.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.lecture.domain.vo.LectureStatus;
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
public class Lecture extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id")
    private Long classId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LectureStatus status;

    @Column(name = "lecture_date")
    private LocalDate lectureDate;

    @Builder
    private Lecture(Long classId, Long teacherId, String title,
                    String description, String youtubeUrl,
                    LectureVisibility visibility, LocalDate lectureDate) {
        this.classId = classId;
        this.teacherId = teacherId;
        this.title = title;
        this.description = description;
        this.youtubeUrl = youtubeUrl;
        this.visibility = visibility != null ? visibility : LectureVisibility.TEACHER_ONLY;
        this.lectureDate = lectureDate;
        this.status = LectureStatus.ACTIVE;
    }

    public static Lecture create(Long classId, Long teacherId,
                                  String title, String description, String youtubeUrl,
                                  LectureVisibility visibility, LocalDate lectureDate) {
        return Lecture.builder()
                .classId(classId)
                .teacherId(teacherId)
                .title(title)
                .description(description)
                .youtubeUrl(youtubeUrl)
                .visibility(visibility)
                .lectureDate(lectureDate)
                .build();
    }

    public void update(String title, String description, LectureVisibility visibility,
                       LectureStatus status, LocalDate lectureDate) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
        if (status != null) {
            this.status = status;
        }
        if (lectureDate != null) {
            this.lectureDate = lectureDate;
        }
    }

    public void close() {
        this.status = LectureStatus.CLOSED;
    }

    public boolean isVisibleToStudents() {
        return this.visibility == LectureVisibility.STUDENT;
    }
}
