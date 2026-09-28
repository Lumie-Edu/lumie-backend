package com.lumie.assignment.application.port.out;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import java.util.List;

public interface AssignmentResultPersistencePort {
    List<AssignmentSubmission> findByResult(Long resultId);
    void deleteAndFlush(List<AssignmentSubmission> submissions);
}
