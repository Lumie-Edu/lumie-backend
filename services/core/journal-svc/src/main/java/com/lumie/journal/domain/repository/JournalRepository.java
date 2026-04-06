package com.lumie.journal.domain.repository;

import com.lumie.journal.domain.entity.ClassJournal;
import com.lumie.journal.domain.vo.JournalSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface JournalRepository {

    ClassJournal save(ClassJournal journal);

    Optional<ClassJournal> findById(Long id);

    Page<ClassJournal> findAll(JournalSearchCriteria criteria, Pageable pageable);

    Page<ClassJournal> findByClassId(Long classId, Pageable pageable);

    Page<ClassJournal> findByTeacherId(Long teacherId, Pageable pageable);

    boolean existsByClassIdAndLessonDate(Long classId, LocalDate lessonDate);
}
