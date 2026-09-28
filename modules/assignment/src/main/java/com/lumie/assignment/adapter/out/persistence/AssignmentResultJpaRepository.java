package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentResultJpaRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByExamResultId(Long resultId);
}
