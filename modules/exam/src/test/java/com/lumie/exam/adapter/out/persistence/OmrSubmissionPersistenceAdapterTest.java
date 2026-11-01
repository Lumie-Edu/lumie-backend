package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OmrSubmissionPersistenceAdapterTest {

    @Mock private OmrSubmissionJpaRepository jpaRepository;
    @InjectMocks private OmrSubmissionPersistenceAdapter adapter;

    @Test
    void everyPortOperationDelegates() throws Exception {
        PersistenceAdapterContractAssertions.everyPortOperationDelegates(
                com.lumie.exam.application.port.out.OmrSubmissionPersistencePort.class,
                OmrSubmissionJpaRepository.class, adapter, jpaRepository);
    }

    @Test
    void delegatesCallbackLookupAndPreservesEmptyResult() {
        when(jpaRepository.findByJobIdAndImageIndex(4L, 2)).thenReturn(Optional.empty());
        when(jpaRepository.findByJobIdAndImageIndexForUpdate(4L, 2)).thenReturn(Optional.empty());

        assertThat(adapter.findByJobIdAndImageIndex(4L, 2)).isEmpty();
        assertThat(adapter.findByJobIdAndImageIndexForUpdate(4L, 2)).isEmpty();

        verify(jpaRepository).findByJobIdAndImageIndex(4L, 2);
        verify(jpaRepository).findByJobIdAndImageIndexForUpdate(4L, 2);
    }

    @Test
    void preservesPessimisticLockAndFetchGraphs() throws Exception {
        var lockMethod = OmrSubmissionJpaRepository.class
                .getMethod("findByJobIdAndImageIndexForUpdate", Long.class, Integer.class);
        assertThat(lockMethod.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);

        var graphMethod = OmrSubmissionJpaRepository.class
                .getMethod("findByJobIdAndImageIndex", Long.class, Integer.class);
        assertThat(graphMethod.getAnnotation(EntityGraph.class).attributePaths())
                .containsExactly("exam", "questions");
    }

    @Test
    void keepsReconciliationAndImageKeyQueryResults() {
        when(jpaRepository.findByExamIdAndReconciliationStatusInOrderByCreatedAtDesc(
                7L, List.of(OmrReconciliationStatus.UNMATCHED))).thenReturn(List.of());
        when(jpaRepository.findSourceImageKeysWithoutPermanentCopyByJobId(4L)).thenReturn(List.of("key"));

        assertThat(adapter.findByExamIdAndReconciliationStatusInOrderByCreatedAtDesc(
                7L, List.of(OmrReconciliationStatus.UNMATCHED))).isEmpty();
        assertThat(adapter.findSourceImageKeysWithoutPermanentCopyByJobId(4L)).containsExactly("key");
    }
}
