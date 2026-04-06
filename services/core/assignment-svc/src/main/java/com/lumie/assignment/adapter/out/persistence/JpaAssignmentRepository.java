package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaAssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

    Page<Assignment> findByClassId(Long classId, Pageable pageable);

    Page<Assignment> findByAcademyId(Long academyId, Pageable pageable);
}
