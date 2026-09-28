package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.application.port.out.AssignmentResultPersistencePort;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentResultPersistenceAdapter implements AssignmentResultPersistencePort {
    private final AssignmentResultJpaRepository repository;

    @Override
    public List<AssignmentSubmission> findByResult(Long resultId) {
        return repository.findByExamResultId(resultId);
    }

    @Override
    public void deleteAndFlush(List<AssignmentSubmission> submissions) {
        repository.deleteAll(submissions);
        repository.flush();
    }
}
