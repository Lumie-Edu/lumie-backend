package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AssignmentRepository {

    Assignment save(Assignment assignment);

    Optional<Assignment> findById(Long id);

    Page<Assignment> findAll(AssignmentSearchCriteria criteria, Pageable pageable);

    Page<Assignment> findByClassId(Long classId, Pageable pageable);

    Page<Assignment> findByAcademyId(Long academyId, Pageable pageable);
}
