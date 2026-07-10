package com.lumie.assignment.application.service;

import com.lumie.assignment.domain.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentLifecycleService {

    private final AssignmentRepository assignmentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int closeOverdueAssignments() {
        return closeOverdueAssignments(Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int closeOverdueAssignments(Instant now) {
        int closed = assignmentRepository.closeOverdueActiveAssignments(now);
        if (closed > 0) {
            log.info("Closed {} overdue assignment(s)", closed);
        }
        return closed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int closeOverdueAssignment(Long assignmentId) {
        return closeOverdueAssignment(assignmentId, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int closeOverdueAssignment(Long assignmentId, Instant now) {
        int closed = assignmentRepository.closeOverdueActiveAssignment(assignmentId, now);
        if (closed > 0) {
            log.info("Closed overdue assignment id={}", assignmentId);
        }
        return closed;
    }
}
