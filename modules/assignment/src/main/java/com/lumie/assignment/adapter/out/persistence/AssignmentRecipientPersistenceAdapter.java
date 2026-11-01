package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.application.port.out.AssignmentRecipientPersistencePort;
import com.lumie.assignment.domain.entity.AssignmentRecipient;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentRecipientPersistenceAdapter implements AssignmentRecipientPersistencePort {
    private final AssignmentRecipientJpaRepository repository;

    @Override public List<AssignmentRecipient> findByAssignmentId(Long id) { return repository.findByAssignmentId(id); }
    @Override public List<AssignmentRecipient> findByAssignmentIdIn(Collection<Long> ids) { return repository.findByAssignmentIdIn(ids); }
    @Override public boolean existsByAssignmentIdAndSourceProvenance(Long id, String provenance) { return repository.existsByAssignmentIdAndSourceProvenance(id, provenance); }
    @Override public boolean existsByAssignmentIdAndStudentId(Long id, Long studentId) { return repository.existsByAssignmentIdAndStudentId(id, studentId); }
    @Override public Optional<AssignmentRecipient> findByAssignmentIdAndStudentId(Long id, Long studentId) { return repository.findByAssignmentIdAndStudentId(id, studentId); }
    @Override public List<Long> findAssignmentIdsByStudentId(Long studentId) { return repository.findAssignmentIdsByStudentId(studentId); }
    @Override public List<Long> findStudentIdsByAssignmentId(Long id) { return repository.findStudentIdsByAssignmentId(id); }
    @Override public List<Long> findAssignmentIdsByStudentIdIn(Collection<Long> ids) { return repository.findAssignmentIdsByStudentIdIn(ids); }
    @Override public List<AssignmentRecipient> saveAll(Collection<AssignmentRecipient> recipients) { return repository.saveAll(recipients); }
}
