package com.lumie.lecture.adapter.out.persistence;

import com.lumie.lecture.application.port.out.LecturePersistencePort;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LecturePersistenceAdapter implements LecturePersistencePort {

    private final LectureJpaRepository jpaRepository;

    @Override
    public Lecture save(Lecture lecture) {
        return jpaRepository.save(lecture);
    }

    @Override
    public Optional<Lecture> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void delete(Lecture lecture) {
        jpaRepository.delete(lecture);
    }

    @Override
    public Page<Lecture> findVisibleByTargetClassId(Long classId, Pageable pageable) {
        return jpaRepository.findVisibleByTargetClassId(classId, pageable);
    }

    @Override
    public Page<Lecture> findAll(LectureSearchCriteria criteria, Pageable pageable) {
        return jpaRepository.findAll(criteria, pageable);
    }
}
