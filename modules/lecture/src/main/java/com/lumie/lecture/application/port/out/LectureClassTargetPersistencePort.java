package com.lumie.lecture.application.port.out;

import com.lumie.lecture.domain.entity.LectureClassTarget;

import java.util.Collection;
import java.util.List;

public interface LectureClassTargetPersistencePort {

    LectureClassTarget save(LectureClassTarget target);

    void flush();

    List<LectureClassTarget> findByLectureIdOrderByClassIdAsc(Long lectureId);

    List<Long> findClassIdsByLectureIdOrderByClassIdAsc(Long lectureId);

    long countByLectureId(Long lectureId);

    void deleteByLectureId(Long lectureId);

    List<LectureClassTargetRow> findRowsByLectureIds(Collection<Long> lectureIds);

    record LectureClassTargetRow(Long lectureId, Long classId) {
    }
}
