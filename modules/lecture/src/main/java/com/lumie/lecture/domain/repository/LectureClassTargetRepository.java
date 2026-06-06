package com.lumie.lecture.domain.repository;

import com.lumie.lecture.domain.entity.LectureClassTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LectureClassTargetRepository extends JpaRepository<LectureClassTarget, Long> {

    List<LectureClassTarget> findByLectureIdOrderByClassIdAsc(Long lectureId);

    @Query("""
            SELECT t.classId
            FROM LectureClassTarget t
            WHERE t.lecture.id = :lectureId
            ORDER BY t.classId ASC
            """)
    List<Long> findClassIdsByLectureIdOrderByClassIdAsc(@Param("lectureId") Long lectureId);

    long countByLectureId(Long lectureId);

    @Modifying(flushAutomatically = true)
    @Query("delete from LectureClassTarget t where t.lecture.id = :lectureId")
    void deleteByLectureId(@Param("lectureId") Long lectureId);

    @Query("""
            SELECT t.lecture.id AS lectureId, t.classId AS classId
            FROM LectureClassTarget t
            WHERE t.lecture.id IN :lectureIds
            ORDER BY t.classId ASC
            """)
    List<LectureClassTargetRow> findRowsByLectureIds(@Param("lectureIds") Collection<Long> lectureIds);

    interface LectureClassTargetRow {
        Long getLectureId();
        Long getClassId();
    }
}
