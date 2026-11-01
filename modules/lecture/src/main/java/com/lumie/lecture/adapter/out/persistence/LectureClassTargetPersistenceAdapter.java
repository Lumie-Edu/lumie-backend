package com.lumie.lecture.adapter.out.persistence;

import com.lumie.lecture.application.port.out.LectureClassTargetPersistencePort;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LectureClassTargetPersistenceAdapter implements LectureClassTargetPersistencePort {

    private final LectureClassTargetJpaRepository jpaRepository;

    @Override
    public LectureClassTarget save(LectureClassTarget target) {
        return jpaRepository.save(target);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public List<LectureClassTarget> findByLectureIdOrderByClassIdAsc(Long lectureId) {
        return jpaRepository.findByLectureIdOrderByClassIdAsc(lectureId);
    }

    @Override
    public List<Long> findClassIdsByLectureIdOrderByClassIdAsc(Long lectureId) {
        return jpaRepository.findClassIdsByLectureIdOrderByClassIdAsc(lectureId);
    }

    @Override
    public long countByLectureId(Long lectureId) {
        return jpaRepository.countByLectureId(lectureId);
    }

    @Override
    public void deleteByLectureId(Long lectureId) {
        jpaRepository.deleteByLectureId(lectureId);
    }

    @Override
    public List<LectureClassTargetRow> findRowsByLectureIds(Collection<Long> lectureIds) {
        return jpaRepository.findRowsByLectureIds(lectureIds).stream()
                .map(row -> new LectureClassTargetRow(row.getLectureId(), row.getClassId()))
                .toList();
    }
}
