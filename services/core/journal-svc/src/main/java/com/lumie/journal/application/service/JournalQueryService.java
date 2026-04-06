package com.lumie.journal.application.service;

import com.lumie.journal.application.dto.JournalResponse;
import com.lumie.journal.domain.entity.ClassJournal;
import com.lumie.journal.domain.exception.JournalNotFoundException;
import com.lumie.journal.domain.repository.JournalRepository;
import com.lumie.journal.domain.vo.JournalSearchCriteria;
import com.lumie.journal.domain.vo.JournalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalQueryService {

    private final JournalRepository journalRepository;

    public JournalResponse getJournal(Long id) {
        ClassJournal journal = journalRepository.findById(id)
            .orElseThrow(() -> new JournalNotFoundException(id));

        return JournalResponse.from(journal);
    }

    public Page<JournalResponse> listJournals(Long classId, Long academyId, Long teacherId,
                                               JournalStatus status, LocalDate startDate,
                                               LocalDate endDate, String search, Pageable pageable) {
        JournalSearchCriteria criteria = new JournalSearchCriteria(
            classId, academyId, teacherId, status, startDate, endDate, search);

        return journalRepository.findAll(criteria, pageable)
            .map(JournalResponse::from);
    }

    public Page<JournalResponse> getJournalsByClass(Long classId, Pageable pageable) {
        return journalRepository.findByClassId(classId, pageable)
            .map(JournalResponse::from);
    }

    public Page<JournalResponse> getJournalsByTeacher(Long teacherId, Pageable pageable) {
        return journalRepository.findByTeacherId(teacherId, pageable)
            .map(JournalResponse::from);
    }
}
