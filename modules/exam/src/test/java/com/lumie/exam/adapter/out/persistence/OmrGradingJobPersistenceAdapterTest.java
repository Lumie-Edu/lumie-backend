package com.lumie.exam.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OmrGradingJobPersistenceAdapterTest {

    @Mock private OmrGradingJobJpaRepository jpaRepository;
    @InjectMocks private OmrGradingJobPersistenceAdapter adapter;

    @Test
    void everyPortOperationDelegates() throws Exception {
        PersistenceAdapterContractAssertions.everyPortOperationDelegates(
                com.lumie.exam.application.port.out.OmrGradingJobPersistencePort.class,
                OmrGradingJobJpaRepository.class, adapter, jpaRepository);
    }

    @Test
    void preservesConditionalCallbackTransitionsAndMutationCounts() {
        when(jpaRepository.markProcessingIfPending(7L)).thenReturn(0);
        when(jpaRepository.markCompletedIfAllProcessed(7L, 3)).thenReturn(0);
        when(jpaRepository.markFailedIfAllProcessed(7L, 3)).thenReturn(1);

        assertThat(adapter.startProcessingIfPending(7L)).isFalse();
        assertThat(adapter.tryFinalize(7L, 3)).isTrue();

        verify(jpaRepository).markProcessingIfPending(7L);
        verify(jpaRepository).markCompletedIfAllProcessed(7L, 3);
        verify(jpaRepository).markFailedIfAllProcessed(7L, 3);
    }

    @Test
    void stopsFinalizationWhenCompletionTransitionWins() {
        when(jpaRepository.markCompletedIfAllProcessed(8L, 2)).thenReturn(1);

        assertThat(adapter.tryFinalize(8L, 2)).isTrue();

        verify(jpaRepository).markCompletedIfAllProcessed(8L, 2);
    }
}
