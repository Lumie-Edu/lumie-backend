package com.lumie.journal.application.service;

import com.lumie.journal.application.dto.CreateJournalRequest;
import com.lumie.journal.application.dto.JournalResponse;
import com.lumie.journal.application.dto.UpdateJournalRequest;
import com.lumie.journal.application.port.out.AcademyServicePort;
import com.lumie.journal.application.port.out.AdminServicePort;
import com.lumie.journal.application.port.out.ClassServicePort;
import com.lumie.journal.domain.entity.ClassJournal;
import com.lumie.journal.domain.exception.JournalErrorCode;
import com.lumie.journal.domain.exception.JournalNotFoundException;
import com.lumie.journal.domain.repository.JournalRepository;
import com.lumie.journal.domain.vo.JournalStatus;
import com.lumie.journal.domain.vo.JournalVisibility;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JournalCommandService {

    private final JournalRepository journalRepository;
    private final ClassServicePort classServicePort;
    private final AcademyServicePort academyServicePort;
    private final AdminServicePort adminServicePort;

    public JournalResponse createJournal(CreateJournalRequest request) {
        classServicePort.getClass(request.classId())
            .orElseThrow(() -> new BusinessException(JournalErrorCode.CLASS_NOT_FOUND,
                "Class not found: " + request.classId()));

        academyServicePort.getAcademy(request.academyId())
            .filter(AcademyServicePort.AcademyInfo::isActive)
            .orElseThrow(() -> new BusinessException(JournalErrorCode.ACADEMY_NOT_FOUND,
                "Academy not found or inactive: " + request.academyId()));

        Long teacherId = request.teacherId() != null ? request.teacherId() : UserContextHolder.getUserId();

        if (journalRepository.existsByClassIdAndLessonDate(request.classId(), request.lessonDate())) {
            throw new BusinessException(JournalErrorCode.DUPLICATE_JOURNAL_DATE,
                "A journal already exists for class " + request.classId() + " on " + request.lessonDate());
        }

        JournalVisibility visibility = parseVisibility(request.visibility());

        ClassJournal journal = ClassJournal.create(
            request.classId(),
            request.academyId(),
            teacherId,
            request.lessonDate(),
            request.title(),
            request.content(),
            request.topicsCovered(),
            request.homeworkNote(),
            visibility,
            request.attachments()
        );

        ClassJournal saved = journalRepository.save(journal);

        log.info("Journal created: {} for class: {} on {}", saved.getId(), saved.getClassId(), saved.getLessonDate());
        return JournalResponse.from(saved);
    }

    public JournalResponse updateJournal(Long id, UpdateJournalRequest request) {
        ClassJournal journal = journalRepository.findById(id)
            .orElseThrow(() -> new JournalNotFoundException(id));

        String title = request.title() != null ? request.title() : journal.getTitle();
        String content = request.content() != null ? request.content() : journal.getContent();
        var topicsCovered = request.topicsCovered() != null ? request.topicsCovered() : journal.getTopicsCovered();
        String homeworkNote = request.homeworkNote() != null ? request.homeworkNote() : journal.getHomeworkNote();
        JournalVisibility visibility = request.visibility() != null
                ? parseVisibility(request.visibility()) : journal.getVisibility();
        var lessonDate = request.lessonDate() != null ? request.lessonDate() : journal.getLessonDate();
        var attachments = request.attachments() != null ? request.attachments() : journal.getAttachments();

        journal.updateInfo(title, content, topicsCovered, homeworkNote, visibility, lessonDate, attachments);

        if (request.status() != null) {
            JournalStatus newStatus = parseJournalStatus(request.status());
            switch (newStatus) {
                case ACTIVE -> journal.activate();
                case CLOSED -> journal.close();
                case DRAFT -> journal.markDraft();
            }
        }

        ClassJournal saved = journalRepository.save(journal);

        log.info("Journal updated: {}", saved.getId());
        return JournalResponse.from(saved);
    }

    public void deleteJournal(Long id) {
        ClassJournal journal = journalRepository.findById(id)
            .orElseThrow(() -> new JournalNotFoundException(id));

        journal.close();
        journalRepository.save(journal);

        log.info("Journal closed (soft-deleted): {}", id);
    }

    private JournalVisibility parseVisibility(String visibility) {
        if (visibility == null) {
            return JournalVisibility.TEACHER_ONLY;
        }
        try {
            return JournalVisibility.valueOf(visibility);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(JournalErrorCode.INVALID_STATUS, "Invalid visibility: " + visibility);
        }
    }

    private JournalStatus parseJournalStatus(String status) {
        try {
            return JournalStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(JournalErrorCode.INVALID_STATUS, "Invalid journal status: " + status);
        }
    }
}
