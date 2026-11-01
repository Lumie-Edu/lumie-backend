package com.lumie.classroom.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ClassEnrollmentPersistenceAdapterTest {

    @Mock private ClassEnrollmentJpaRepository repository;
    @InjectMocks private ClassEnrollmentPersistenceAdapter adapter;

    @Test
    void delegatesEveryEnrollmentQueryAndCrudOperationWithoutChangingResults() {
        var pageable = PageRequest.of(1, 7);
        var page = new PageImpl<ClassEnrollment>(List.of(), pageable, 0);
        var status = EnrollmentStatus.ENROLLED;
        var ids = List.of(10L, 11L);
        var emptyPage = Page.<ClassEnrollment>empty(pageable);
        when(repository.findByClassEntityId(4L, pageable)).thenReturn(page);
        when(repository.findByStudentId(8L)).thenReturn(List.of());
        when(repository.findByStudentIdAndStatus(8L, status)).thenReturn(List.of());
        when(repository.findByStudentIdInAndStatus(ids, status)).thenReturn(List.of());
        when(repository.findByClassEntityIdAndStatus(4L, status)).thenReturn(List.of());
        when(repository.findStudentIdsByStatus(status)).thenReturn(List.of());
        when(repository.findAllByStudentIdAndStatus(8L, status)).thenReturn(List.of());
        when(repository.findByStudentId(8L, pageable)).thenReturn(emptyPage);
        when(repository.findByStudentIdAndStatus(8L, status, pageable)).thenReturn(emptyPage);

        assertThat(adapter.findByClassEntityId(4L, pageable)).isSameAs(page);
        assertThat(adapter.findByStudentId(8L)).isEmpty();
        assertThat(adapter.findByStudentIdAndStatus(8L, status)).isEmpty();
        assertThat(adapter.findByStudentIdInAndStatus(ids, status)).isEmpty();
        assertThat(adapter.findByStudentId(8L, pageable).getContent()).isEmpty();
        assertThat(adapter.findByStudentIdAndStatus(8L, status, pageable).getContent()).isEmpty();
        assertThat(adapter.findByClassEntityIdAndStatus(4L, status)).isEmpty();
        assertThat(adapter.findStudentIdsByStatus(status)).isEmpty();
        assertThat(adapter.countByClassEntityIdAndStatus(4L, status)).isZero();
        assertThat(adapter.existsByClassEntityIdAndStudentId(4L, 8L)).isFalse();
        assertThat(adapter.existsByClassEntityIdAndStudentIdAndStatus(4L, 8L, status)).isFalse();
        assertThat(adapter.existsByStudentIdAndStatus(8L, status)).isFalse();
        assertThat(adapter.findAllByStudentIdAndStatus(8L, status)).isEmpty();
        assertThat(adapter.findByClassEntityIdAndStudentId(4L, 8L)).isEmpty();
        assertThat(adapter.findById(5L)).isEmpty();
        var enrollment = mock(ClassEnrollment.class);
        adapter.save(enrollment);
        adapter.saveAll(List.of());
        adapter.delete(enrollment);

        verify(repository).findByClassEntityId(4L, pageable);
        verify(repository).findByStudentId(8L);
        verify(repository).findByStudentIdAndStatus(8L, status);
        verify(repository).findByStudentIdInAndStatus(ids, status);
        verify(repository).findByStudentId(8L, pageable);
        verify(repository).findByStudentIdAndStatus(8L, status, pageable);
        verify(repository).findByClassEntityIdAndStatus(4L, status);
        verify(repository).findStudentIdsByStatus(status);
        verify(repository).countByClassEntityIdAndStatus(4L, status);
        verify(repository).existsByClassEntityIdAndStudentId(4L, 8L);
        verify(repository).existsByClassEntityIdAndStudentIdAndStatus(4L, 8L, status);
        verify(repository).existsByStudentIdAndStatus(8L, status);
        verify(repository).findAllByStudentIdAndStatus(8L, status);
        verify(repository).findByClassEntityIdAndStudentId(4L, 8L);
        verify(repository).findById(5L);
        verify(repository).save(enrollment);
        verify(repository).saveAll(List.of());
        verify(repository).delete(enrollment);
    }

    @Test
    void preservesRepositoryFailures() {
        var failure = new IllegalStateException("database unavailable");
        when(repository.findByStudentId(8L)).thenThrow(failure);

        assertThatThrownBy(() -> adapter.findByStudentId(8L)).isSameAs(failure);
    }
}
