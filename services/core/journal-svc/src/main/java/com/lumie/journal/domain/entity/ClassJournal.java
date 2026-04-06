package com.lumie.journal.domain.entity;

import com.lumie.journal.domain.vo.JournalStatus;
import com.lumie.journal.domain.vo.JournalVisibility;
import com.lumie.journal.domain.vo.ProcessingStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "class_journals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassJournal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_script", columnDefinition = "TEXT")
    private String aiScript;

    @Column(name = "media_file_id")
    private Long mediaFileId;

    @Convert(converter = LongListConverter.class)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Long> attachments;

    @Convert(converter = StringListConverter.class)
    @Column(name = "topics_covered", columnDefinition = "jsonb")
    private List<String> topicsCovered;

    @Column(name = "homework_note", columnDefinition = "TEXT")
    private String homeworkNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private JournalVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JournalStatus status;

    @Builder
    private ClassJournal(Long classId, Long academyId, Long teacherId, LocalDate lessonDate,
                         String title, String content, String aiSummary, String aiScript,
                         Long mediaFileId, List<Long> attachments, List<String> topicsCovered,
                         String homeworkNote, JournalVisibility visibility,
                         ProcessingStatus processingStatus, JournalStatus status) {
        this.classId = classId;
        this.academyId = academyId;
        this.teacherId = teacherId;
        this.lessonDate = lessonDate;
        this.title = title;
        this.content = content;
        this.aiSummary = aiSummary;
        this.aiScript = aiScript;
        this.mediaFileId = mediaFileId;
        this.attachments = attachments;
        this.topicsCovered = topicsCovered;
        this.homeworkNote = homeworkNote;
        this.visibility = visibility != null ? visibility : JournalVisibility.TEACHER_ONLY;
        this.processingStatus = processingStatus != null ? processingStatus : ProcessingStatus.COMPLETED;
        this.status = status != null ? status : JournalStatus.ACTIVE;
    }

    public static ClassJournal create(Long classId, Long academyId, Long teacherId,
                                       LocalDate lessonDate, String title, String content,
                                       List<String> topicsCovered, String homeworkNote,
                                       JournalVisibility visibility, List<Long> attachments) {
        return ClassJournal.builder()
                .classId(classId)
                .academyId(academyId)
                .teacherId(teacherId)
                .lessonDate(lessonDate)
                .title(title)
                .content(content)
                .topicsCovered(topicsCovered)
                .homeworkNote(homeworkNote)
                .visibility(visibility != null ? visibility : JournalVisibility.TEACHER_ONLY)
                .processingStatus(ProcessingStatus.COMPLETED)
                .status(JournalStatus.ACTIVE)
                .attachments(attachments)
                .build();
    }

    public void updateInfo(String title, String content, List<String> topicsCovered,
                           String homeworkNote, JournalVisibility visibility,
                           LocalDate lessonDate, List<Long> attachments) {
        this.title = title;
        this.content = content;
        this.topicsCovered = topicsCovered;
        this.homeworkNote = homeworkNote;
        this.visibility = visibility;
        this.lessonDate = lessonDate;
        this.attachments = attachments;
    }

    public void close() {
        this.status = JournalStatus.CLOSED;
    }

    public void activate() {
        this.status = JournalStatus.ACTIVE;
    }

    public void markDraft() {
        this.status = JournalStatus.DRAFT;
    }

    public boolean isActive() {
        return this.status == JournalStatus.ACTIVE;
    }
}
