package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.vo.JobStatus;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGenerationJobPersistenceAdapterTest {

    @Mock private ReportGenerationJobJpaRepository jpaRepository;
    @InjectMocks private ReportGenerationJobPersistenceAdapter adapter;

    @Test
    void everyPortOperationDelegates() throws Exception {
        PersistenceAdapterContractAssertions.everyPortOperationDelegates(
                com.lumie.exam.application.port.out.ReportGenerationJobPersistencePort.class,
                ReportGenerationJobJpaRepository.class, adapter, jpaRepository);
    }

    @Test
    void preservesJobLockAndStaleJobLockSemantics() throws Exception {
        var lockMethod = ReportGenerationJobJpaRepository.class.getMethod("findByIdForUpdate", Long.class);
        assertThat(lockMethod.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        var staleMethod = ReportGenerationJobJpaRepository.class.getMethod(
                "findStaleOpenJobsForUpdate", java.util.Collection.class, Instant.class);
        assertThat(staleMethod.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void keepsConditionalStateTransitionsAndMutationCounts() {
        when(jpaRepository.markProcessingIfPending(7L)).thenReturn(0);
        when(jpaRepository.markCompletedIfAllProcessed(7L, 2)).thenReturn(0);
        when(jpaRepository.markFailedIfAllProcessed(7L, 2)).thenReturn(1);

        assertThat(adapter.startProcessingIfPending(7L)).isFalse();
        assertThat(adapter.tryFinalize(7L, 2)).isTrue();

        verify(jpaRepository).markProcessingIfPending(7L);
        verify(jpaRepository).markCompletedIfAllProcessed(7L, 2);
        verify(jpaRepository).markFailedIfAllProcessed(7L, 2);
    }

    @Test
    void preservesEmptyExamJobListsAndStaleSelectionArguments() {
        Instant cutoff = Instant.parse("2026-10-01T00:00:00Z");
        when(jpaRepository.findByExamIdOrderByCreatedAtDesc(3L)).thenReturn(List.of());
        when(jpaRepository.findStaleOpenJobsForUpdate(List.of(JobStatus.PENDING), cutoff)).thenReturn(List.of());

        assertThat(adapter.findByExamIdOrderByCreatedAtDesc(3L)).isEmpty();
        assertThat(adapter.findStaleOpenJobsForUpdate(List.of(JobStatus.PENDING), cutoff)).isEmpty();
    }
}
