package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ExamResultPersistencePort;
import com.lumie.exam.domain.entity.ExamResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamResultPersistenceAdapterTest {

    @Mock
    private ExamResultJpaRepository jpaRepository;

    @InjectMocks
    private ExamResultPersistenceAdapter adapter;

    @Test
    void everyPortOperationDelegates() throws Exception {
        PersistenceAdapterContractAssertions.everyPortOperationDelegates(
                ExamResultPersistencePort.class, ExamResultJpaRepository.class, adapter, jpaRepository);
    }

    @Test
    void preservesOptionalAndFetchQueryResults() {
        when(jpaRepository.findByExamIdAndStudentId(8L, 3L)).thenReturn(Optional.empty());
        when(jpaRepository.findWithQuestionResultsById(5L)).thenReturn(Optional.empty());
        when(jpaRepository.findByExamId(8L)).thenReturn(List.of());

        assertThat(adapter.findByExamIdAndStudentId(8L, 3L)).isEmpty();
        assertThat(adapter.findWithQuestionResultsById(5L)).isEmpty();
        assertThat(adapter.findByExamId(8L)).isEmpty();

        verify(jpaRepository).findByExamIdAndStudentId(8L, 3L);
        verify(jpaRepository).findWithQuestionResultsById(5L);
        verify(jpaRepository).findByExamId(8L);
    }

    @Test
    void retainsQueryFetchGraphsForStudentAndExamResultLookups() throws Exception {
        var studentLookup = ExamResultJpaRepository.class
                .getMethod("findByExamIdAndStudentId", Long.class, Long.class);
        assertThat(studentLookup.getAnnotation(EntityGraph.class).attributePaths()).containsExactly("exam");

        var examLookup = ExamResultJpaRepository.class.getMethod("findByExamId", Long.class);
        assertThat(examLookup.getAnnotation(EntityGraph.class).attributePaths())
                .containsExactly("exam", "questionResults");
    }

    @Test
    void preservesProjectionValuesAndAggregateAbsence() {
        var row = org.mockito.Mockito.mock(ExamResultPersistencePort.JobSavedCountRow.class);
        when(jpaRepository.countOfficializedByJobIds(List.of(4L))).thenReturn(List.of(row));
        when(jpaRepository.aggregateGradeBuckets()).thenReturn(List.of());

        assertThat(adapter.countOfficializedByJobIds(List.of(4L))).containsExactly(row);
        assertThat(adapter.aggregateGradeBuckets()).isEmpty();
    }

    @Test
    void delegatesWritesAndPropagatesPersistenceFailure() {
        ExamResult result = org.mockito.Mockito.mock(ExamResult.class);
        when(jpaRepository.save(result)).thenReturn(result);
        doThrow(new IllegalStateException("database unavailable")).when(jpaRepository).deleteById(9L);

        assertThat(adapter.save(result)).isSameAs(result);
        assertThatThrownBy(() -> adapter.deleteById(9L)).isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }
}
