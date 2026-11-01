package com.lumie.classroom.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.vo.ClassSearchCriteria;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ClassPersistenceAdapterTest {

    @Mock private ClassJpaRepository repository;
    @InjectMocks private ClassPersistenceAdapter adapter;

    @Test
    void delegatesEveryClassQueryAndCrudOperationWithSearchCriteriaAndPaging() {
        var pageable = PageRequest.of(2, 5);
        var page = new PageImpl<ClassEntity>(List.of(), pageable, 0);
        var criteria = new ClassSearchCriteria(12L, "math");
        var status = EnrollmentStatus.ENROLLED;
        var ids = List.of(1L, 2L);
        when(repository.findByTeacherId(12L, pageable)).thenReturn(page);
        when(repository.findEnrolledClassesByStudentIdAndStatus(7L, status, pageable)).thenReturn(page);
        when(repository.findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(page);

        assertThat(adapter.findByIdWithLock(3L)).isEmpty();
        assertThat(adapter.findByTeacherId(12L, pageable)).isSameAs(page);
        assertThat(adapter.findEnrolledClassesByStudentIdAndStatus(7L, status, pageable)).isSameAs(page);
        assertThat(adapter.findAllByTeacherId(12L)).isEmpty();
        assertThat(adapter.existsByTeacherId(12L)).isFalse();
        assertThat(adapter.findAll(criteria, pageable)).isSameAs(page);
        assertThat(adapter.findById(3L)).isEmpty();
        assertThat(adapter.findAllById(ids)).isEmpty();
        assertThat(adapter.count()).isZero();
        var entity = org.mockito.Mockito.mock(ClassEntity.class);
        adapter.save(entity);
        adapter.delete(entity);

        verify(repository).findByIdWithLock(3L);
        verify(repository).findByTeacherId(12L, pageable);
        verify(repository).findEnrolledClassesByStudentIdAndStatus(7L, status, pageable);
        verify(repository).findAllByTeacherId(12L);
        verify(repository).existsByTeacherId(12L);
        verify(repository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable));
        verify(repository).findById(3L);
        verify(repository).findAllById(ids);
        verify(repository).count();
        verify(repository).save(entity);
        verify(repository).delete(entity);
    }

    @Test
    void preservesRepositoryFailures() {
        var failure = new IllegalStateException("database unavailable");
        when(repository.findByIdWithLock(3L)).thenThrow(failure);

        assertThatThrownBy(() -> adapter.findByIdWithLock(3L)).isSameAs(failure);
    }

    @Test
    void appliesOptionalTeacherAndCaseInsensitiveSearchFilters() {
        var pageable = PageRequest.of(0, 5);
        when(repository.findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(Page.empty(pageable));

        adapter.findAll(new ClassSearchCriteria(12L, "Math"), pageable);

        var root = mock(Root.class);
        var query = mock(CriteriaQuery.class);
        var builder = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked") Path<Object> teacherPath = mock(Path.class);
        @SuppressWarnings("unchecked") Path<String> namePath = mock(Path.class);
        @SuppressWarnings("unchecked") Expression<String> lowerName = mock(Expression.class);
        var teacherPredicate = mock(Predicate.class);
        var searchPredicate = mock(Predicate.class);
        var combined = mock(Predicate.class);
        when(root.get("teacherId")).thenReturn(teacherPath);
        when(root.get("name")).thenReturn(namePath);
        when(builder.equal(teacherPath, 12L)).thenReturn(teacherPredicate);
        when(builder.lower(namePath)).thenReturn(lowerName);
        when(builder.like(lowerName, "%math%")).thenReturn(searchPredicate);
        when(builder.and(any(Predicate[].class))).thenReturn(combined);

        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(specificationCaptor.capture(), org.mockito.ArgumentMatchers.eq(pageable));
        @SuppressWarnings("unchecked") Specification<ClassEntity> specification = specificationCaptor.getValue();
        assertThat(specification.toPredicate(root, query, builder)).isSameAs(combined);
        verify(builder).equal(teacherPath, 12L);
        verify(builder).like(lowerName, "%math%");
        verify(builder).and(any(Predicate[].class));
    }

    @Test
    void skipsMissingOrBlankSearchFilters() {
        var pageable = PageRequest.of(0, 5);
        when(repository.findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(Page.empty(pageable));
        adapter.findAll(new ClassSearchCriteria(null, null), pageable);
        adapter.findAll(new ClassSearchCriteria(null, "  "), pageable);

        var root = mock(Root.class);
        var query = mock(CriteriaQuery.class);
        var builder = mock(CriteriaBuilder.class);
        var combined = mock(Predicate.class);
        when(builder.and(any(Predicate[].class))).thenReturn(combined);

        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(repository, times(2)).findAll(specificationCaptor.capture(), org.mockito.ArgumentMatchers.eq(pageable));
        @SuppressWarnings("unchecked") var specifications =
                (List<Specification<ClassEntity>>) (List<?>) specificationCaptor.getAllValues();
        var absentSearch = specifications.get(0);
        var blankSearch = specifications.get(1);
        assertThat(absentSearch.toPredicate(root, query, builder)).isSameAs(combined);
        assertThat(blankSearch.toPredicate(root, query, builder)).isSameAs(combined);
        verify(builder, times(2)).and(new Predicate[0]);
        verify(root, org.mockito.Mockito.never()).get("teacherId");
        verify(root, org.mockito.Mockito.never()).get("name");
    }

}
