package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.AssignmentClassTarget;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentClassTargetJpaRepository extends JpaRepository<AssignmentClassTarget, Long> {
    List<AssignmentClassTarget> findByAssignmentIdIn(Collection<Long> assignmentIds);

    Optional<AssignmentClassTarget> findByAssignmentIdAndClassId(Long assignmentId, Long classId);
}
