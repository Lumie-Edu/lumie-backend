package com.lumie.lecture.domain.repository;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LectureRepository {

    Lecture save(Lecture lecture);

    Optional<Lecture> findById(Long id);

    Page<Lecture> findAll(LectureSearchCriteria criteria, Pageable pageable);

    Page<Lecture> findVisibleByClassId(Long classId, Pageable pageable);

    void deleteById(Long id);
}
