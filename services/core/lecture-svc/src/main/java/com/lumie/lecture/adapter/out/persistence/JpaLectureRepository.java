package com.lumie.lecture.adapter.out.persistence;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureStatus;
import com.lumie.lecture.domain.vo.LectureVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLectureRepository extends JpaRepository<Lecture, Long> {

    @Query("SELECT l FROM Lecture l WHERE l.classId = :classId " +
           "AND l.status = 'ACTIVE' " +
           "AND l.visibility IN ('STUDENT', 'PARENT')")
    Page<Lecture> findVisibleByClassId(@Param("classId") Long classId, Pageable pageable);
}
