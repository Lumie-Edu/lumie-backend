package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.AssignmentTarget;
import com.lumie.assignment.domain.vo.AssignmentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AssignmentTargetRepository extends JpaRepository<AssignmentTarget, Long> {

    List<AssignmentTarget> findByAssignmentId(Long assignmentId);

    List<AssignmentTarget> findByAssignmentIdIn(Collection<Long> assignmentIds);

    void deleteByAssignmentId(Long assignmentId);

    boolean existsByAssignmentIdAndTargetTypeAndTargetId(Long assignmentId, AssignmentTargetType targetType, Long targetId);

    @Query("""
            select distinct target.assignmentId
            from AssignmentTarget target
            where (target.targetType = com.lumie.assignment.domain.vo.AssignmentTargetType.STUDENT
                    and target.targetId = :studentId)
               or (target.targetType = com.lumie.assignment.domain.vo.AssignmentTargetType.CLASS
                    and target.targetId in :classIds)
            """)
    List<Long> findVisibleAssignmentIds(@Param("studentId") Long studentId,
                                        @Param("classIds") Collection<Long> classIds);
}
