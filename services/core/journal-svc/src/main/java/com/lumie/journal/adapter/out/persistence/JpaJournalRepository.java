package com.lumie.journal.adapter.out.persistence;

import com.lumie.journal.domain.entity.ClassJournal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface JpaJournalRepository extends JpaRepository<ClassJournal, Long>, JpaSpecificationExecutor<ClassJournal> {

    Page<ClassJournal> findByClassId(Long classId, Pageable pageable);

    Page<ClassJournal> findByTeacherId(Long teacherId, Pageable pageable);

    boolean existsByClassIdAndLessonDate(Long classId, LocalDate lessonDate);
}
