package com.lumie.lecture.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.lecture.domain.vo.LectureStatus;
import com.lumie.lecture.domain.vo.LectureVisibility;
import com.lumie.lecture.domain.vo.ProcessingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "lectures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Video metadata
    @Column(name = "video_file_key", length = 500)
    private String videoFileKey;

    @Column(name = "video_file_name", length = 255)
    private String videoFileName;

    @Column(name = "video_file_size")
    private Long videoFileSize;

    @Column(name = "video_duration")
    private Integer videoDuration;

    @Column(name = "video_mime_type", length = 50)
    private String videoMimeType;

    // Processing results
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "subtitle_file_key", length = 500)
    private String subtitleFileKey;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topics_extracted", columnDefinition = "jsonb")
    private List<String> topicsExtracted;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProcessingStatus processingStatus;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private LectureVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LectureStatus status;

    // Upload tracking
    @Column(name = "upload_id", length = 500)
    private String uploadId;

    @Column(name = "upload_completed", nullable = false)
    private boolean uploadCompleted;

    @Column(name = "lecture_date")
    private LocalDate lectureDate;

    @Builder
    private Lecture(Long classId, Long academyId, Long teacherId, String title,
                    String description, LectureVisibility visibility, LocalDate lectureDate) {
        this.classId = classId;
        this.academyId = academyId;
        this.teacherId = teacherId;
        this.title = title;
        this.description = description;
        this.visibility = visibility != null ? visibility : LectureVisibility.TEACHER_ONLY;
        this.lectureDate = lectureDate;
        this.status = LectureStatus.ACTIVE;
        this.processingStatus = ProcessingStatus.UPLOADING;
        this.uploadCompleted = false;
    }

    public static Lecture create(Long classId, Long academyId, Long teacherId,
                                  String title, String description,
                                  LectureVisibility visibility, LocalDate lectureDate) {
        return Lecture.builder()
                .classId(classId)
                .academyId(academyId)
                .teacherId(teacherId)
                .title(title)
                .description(description)
                .visibility(visibility)
                .lectureDate(lectureDate)
                .build();
    }

    public void initiateUpload(String uploadId, String videoFileKey, String videoFileName,
                                Long videoFileSize, String videoMimeType) {
        this.uploadId = uploadId;
        this.videoFileKey = videoFileKey;
        this.videoFileName = videoFileName;
        this.videoFileSize = videoFileSize;
        this.videoMimeType = videoMimeType;
    }

    public void completeUpload() {
        this.uploadCompleted = true;
        this.processingStatus = ProcessingStatus.PENDING_PROCESSING;
        this.uploadId = null;
    }

    public void updateProcessingStatus(ProcessingStatus status) {
        this.processingStatus = status;
        if (status != ProcessingStatus.FAILED) {
            this.processingError = null;
        }
    }

    public void completeProcessing(String transcript, String subtitleFileKey,
                                    String aiSummary, List<String> topicsExtracted,
                                    Integer videoDuration) {
        this.transcript = transcript;
        this.subtitleFileKey = subtitleFileKey;
        this.aiSummary = aiSummary;
        this.topicsExtracted = topicsExtracted;
        this.videoDuration = videoDuration;
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.processingError = null;
    }

    public void failProcessing(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.processingError = errorMessage;
    }

    public void requestReprocessing() {
        this.processingStatus = ProcessingStatus.PENDING_PROCESSING;
        this.processingError = null;
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
        return this.visibility == LectureVisibility.STUDENT
                || this.visibility == LectureVisibility.PARENT;
    }

    public boolean isProcessingComplete() {
        return this.processingStatus == ProcessingStatus.COMPLETED;
    }
}
