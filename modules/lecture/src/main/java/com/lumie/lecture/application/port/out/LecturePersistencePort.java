package com.lumie.lecture.application.port.out;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LecturePersistencePort {

    Lecture save(Lecture lecture);

    Optional<Lecture> findById(Long id);

    void delete(Lecture lecture);

    Page<Lecture> findVisibleByTargetClassId(Long classId, Pageable pageable);

    Page<Lecture> findAll(LectureSearchCriteria criteria, Pageable pageable);
}
