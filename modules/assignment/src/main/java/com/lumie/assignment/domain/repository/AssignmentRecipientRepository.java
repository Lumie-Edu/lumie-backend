package com.lumie.assignment.domain.repository;

import com.lumie.assignment.domain.entity.AssignmentRecipient;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRecipientRepository extends JpaRepository<AssignmentRecipient, Long> {

    List<AssignmentRecipient> findByAssignmentId(Long assignmentId);

    List<AssignmentRecipient> findByAssignmentIdIn(Collection<Long> assignmentIds);

    boolean existsByAssignmentIdAndSourceProvenance(Long assignmentId, String sourceProvenance);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    Optional<AssignmentRecipient> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("""
            select distinct recipient.assignmentId
            from AssignmentRecipient recipient
            where recipient.studentId = :studentId
            """)
    List<Long> findAssignmentIdsByStudentId(@Param("studentId") Long studentId);

    @Query("""
            select recipient.studentId
            from AssignmentRecipient recipient
            where recipient.assignmentId = :assignmentId
            """)
    List<Long> findStudentIdsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Query("""
            select recipient.assignmentId
            from AssignmentRecipient recipient
            where recipient.studentId in :studentIds
            """)
    List<Long> findAssignmentIdsByStudentIdIn(@Param("studentIds") Collection<Long> studentIds);
}
