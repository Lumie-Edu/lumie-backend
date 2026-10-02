package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.application.port.out.AssignmentRecipientSourcePersistencePort;
import com.lumie.assignment.domain.entity.AssignmentRecipientSource;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentRecipientSourcePersistenceAdapter implements AssignmentRecipientSourcePersistencePort {

    private final AssignmentRecipientSourceJpaRepository repository;

    @Override
    public void recordClassSource(Long recipientId, Long assignmentId, Long studentId,
                                  Long classTargetId, boolean confirmExisting) {
        repository.findByRecipientIdAndClassTargetId(recipientId, classTargetId)
                .ifPresentOrElse(source -> {
                    if (confirmExisting) {
                        source.confirmExact();
                    }
                }, () -> repository.save(AssignmentRecipientSource.forClass(
                        recipientId, assignmentId, studentId, classTargetId)));
    }

    @Override
    public void recordStudentSource(Long recipientId, Long assignmentId, Long studentId,
                                    Long studentTargetId, boolean confirmExisting) {
        repository.findByRecipientIdAndStudentTargetId(recipientId, studentTargetId)
                .ifPresentOrElse(source -> {
                    if (confirmExisting) {
                        source.confirmExact();
                    }
                }, () -> repository.save(AssignmentRecipientSource.forStudent(
                        recipientId, assignmentId, studentId, studentTargetId)));
    }

    @Override
    public boolean hasSource(Long recipientId) {
        return repository.existsByRecipientId(recipientId);
    }

    @Override
    public List<Long> findEligibleAssignmentIdsByStudentId(Long studentId) {
        return repository.findEligibleAssignmentIdsByStudentId(studentId);
    }
}
