package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGenerationItemPersistenceAdapterTest {

    @Mock private ReportGenerationItemJpaRepository jpaRepository;
    @InjectMocks private ReportGenerationItemPersistenceAdapter adapter;

    @Test
    void everyPortOperationDelegates() throws Exception {
        PersistenceAdapterContractAssertions.everyPortOperationDelegates(
                com.lumie.exam.application.port.out.ReportGenerationItemPersistencePort.class,
                ReportGenerationItemJpaRepository.class, adapter, jpaRepository);
    }

    @Test
    void preservesLockedAbsenceAndOrderedEmptyRead() {
        when(jpaRepository.findByJobIdAndReportIndexForUpdate(9L, 2)).thenReturn(Optional.empty());
        when(jpaRepository.findByJobIdOrderByReportIndexAsc(9L)).thenReturn(List.of());

        assertThat(adapter.findByJobIdAndReportIndexForUpdate(9L, 2)).isEmpty();
        assertThat(adapter.findByJobIdOrderByReportIndexAsc(9L)).isEmpty();

        verify(jpaRepository).findByJobIdAndReportIndexForUpdate(9L, 2);
    }

    @Test
    void retainsPessimisticWriteLockForCallbackItemLookup() throws Exception {
        var method = ReportGenerationItemJpaRepository.class
                .getMethod("findByJobIdAndReportIndexForUpdate", Long.class, Integer.class);
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void preservesStatusFilterAndReportOrder() {
        when(jpaRepository.findByJobIdAndStatusOrderByReportIndexAsc(9L, ReportGenerationItemStatus.PENDING))
                .thenReturn(List.of());

        assertThat(adapter.findByJobIdAndStatusOrderByReportIndexAsc(
                9L, ReportGenerationItemStatus.PENDING)).isEmpty();
    }
}
