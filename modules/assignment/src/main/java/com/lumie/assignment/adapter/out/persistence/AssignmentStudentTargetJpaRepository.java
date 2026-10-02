package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.AssignmentStudentTarget;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentStudentTargetJpaRepository extends JpaRepository<AssignmentStudentTarget, Long> {
    List<AssignmentStudentTarget> findByAssignmentIdIn(Collection<Long> assignmentIds);

    Optional<AssignmentStudentTarget> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
