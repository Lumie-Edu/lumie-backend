package com.lumie.assignment.application.port.out;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.vo.AssignmentSearchCriteria;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentPersistencePort {
    Page<Assignment> findByTargetClassId(Long classId, Pageable pageable);
    List<Assignment> findByIdIn(Collection<Long> ids);
    List<Assignment> findActiveByTargetClassId(Long classId, Instant now);
    int closeOverdueActiveAssignments(Instant now);
    int closeOverdueActiveAssignment(Long assignmentId, Instant now);
    Page<Assignment> findAll(AssignmentSearchCriteria criteria, Pageable pageable);
    Optional<Assignment> findById(Long id);
    Assignment save(Assignment assignment);
    void delete(Assignment assignment);
    void flush();
}
