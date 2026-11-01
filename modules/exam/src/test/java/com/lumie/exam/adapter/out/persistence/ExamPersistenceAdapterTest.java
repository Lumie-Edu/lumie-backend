package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamSource;
import com.lumie.exam.application.port.out.ExamPersistencePort;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamPersistenceAdapterTest {

    @Mock
    private ExamJpaRepository jpaRepository;

    @InjectMocks
    private ExamPersistenceAdapter adapter;

    @Test
    void everyPortOperationDelegates() throws Exception {
        PersistenceAdapterContractAssertions.everyPortOperationDelegates(
                ExamPersistencePort.class, ExamJpaRepository.class, adapter, jpaRepository);
    }

    @Test
    void delegatesFindsIncludingEmptyAndLockedLookup() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.empty());
        when(jpaRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(1L)).isEmpty();
        assertThat(adapter.findByIdForUpdate(2L)).isEmpty();

        verify(jpaRepository).findById(1L);
        verify(jpaRepository).findByIdForUpdate(2L);
    }

    @Test
    void retainsPessimisticWriteLockForExamMutationLookup() throws Exception {
        var method = ExamJpaRepository.class.getMethod("findByIdForUpdate", Long.class);
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void delegatesBothSourceQueriesWithoutChangingTheirPageShape() {
        var pageable = PageRequest.of(1, 5);
        Page<Exam> page = org.springframework.data.domain.Page.empty(pageable);
        when(jpaRepository.findBySource(ExamSource.EXAM_MANAGEMENT, pageable)).thenReturn(page);
        when(jpaRepository.findBySource(ExamSource.EXAM_MANAGEMENT)).thenReturn(List.of());

        assertThat(adapter.findBySource(ExamSource.EXAM_MANAGEMENT, pageable)).isSameAs(page);
        assertThat(adapter.findBySource(ExamSource.EXAM_MANAGEMENT)).isEmpty();
    }

    @Test
    void delegatesWritesAndDoesNotHidePersistenceFailures() {
        Exam exam = org.mockito.Mockito.mock(Exam.class);
        when(jpaRepository.save(exam)).thenReturn(exam);
        doThrow(new IllegalStateException("database unavailable")).when(jpaRepository).flush();

        assertThat(adapter.save(exam)).isSameAs(exam);
        adapter.delete(exam);
        assertThatThrownBy(adapter::flush).isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(jpaRepository).save(exam);
        verify(jpaRepository).delete(exam);
    }
}
