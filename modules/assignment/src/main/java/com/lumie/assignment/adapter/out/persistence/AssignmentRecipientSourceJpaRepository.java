package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.domain.entity.AssignmentRecipientSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRecipientSourceJpaRepository extends JpaRepository<AssignmentRecipientSource, Long> {

    Optional<AssignmentRecipientSource> findByRecipientIdAndClassTargetId(Long recipientId, Long classTargetId);

    Optional<AssignmentRecipientSource> findByRecipientIdAndStudentTargetId(Long recipientId, Long studentTargetId);

    boolean existsByRecipientId(Long recipientId);

    @Query("select distinct source.assignmentId from AssignmentRecipientSource source where source.studentId = :studentId")
    List<Long> findEligibleAssignmentIdsByStudentId(@Param("studentId") Long studentId);
}
