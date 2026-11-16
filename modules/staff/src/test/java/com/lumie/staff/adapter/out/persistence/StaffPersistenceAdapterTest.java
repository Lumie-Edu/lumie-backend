package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.Staff;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffPersistenceAdapterTest {
    private final StaffJpaRepository repository = mock(StaffJpaRepository.class);
    private final StaffPersistenceAdapter adapter = new StaffPersistenceAdapter(repository);

    @Test
    void delegatesAllLookupsCountsAndMutations() {
        Staff staff = Staff.create(1L, "staff1", "Name", null, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(staff));
        when(repository.findByUserId(1L)).thenReturn(Optional.of(staff));
        when(repository.findByUserLoginId("staff1")).thenReturn(Optional.of(staff));
        when(repository.existsByUserLoginId("staff1")).thenReturn(true);
        when(repository.existsByCanonicalPhone("01012345678")).thenReturn(true);
        when(repository.existsByCanonicalPhoneAndIdNot("01012345678", 1L)).thenReturn(false);
        when(repository.countByIsActive(true)).thenReturn(1L);
        when(repository.countGroupedByRoleAndIsActive(true)).thenReturn(java.util.Collections.singletonList(new Object[]{"MANAGER", 1L}));
        when(repository.save(staff)).thenReturn(staff);

        assertThat(adapter.findById(1L)).contains(staff);
        assertThat(adapter.findByUserId(1L)).contains(staff);
        assertThat(adapter.findByUserLoginId("staff1")).contains(staff);
        assertThat(adapter.existsByUserLoginId("staff1")).isTrue();
        assertThat(adapter.existsByCanonicalPhone("01012345678")).isTrue();
        assertThat(adapter.existsByCanonicalPhoneAndIdNot("01012345678", 1L)).isFalse();
        assertThat(adapter.countByIsActive(true)).isEqualTo(1L);
        assertThat(adapter.countActiveGroupedByRole()).hasSize(1);
        assertThat(adapter.save(staff)).isSameAs(staff);
        adapter.delete(staff);
        verify(repository).delete(staff);
    }

    @Test
    void searchPreservesFilteringPaginationAndNameSortTranslation() {
        Staff staff = Staff.create(1L, "staff1", "Name", null, null, null);
        PageRequest requested = PageRequest.of(2, 7, Sort.by(Sort.Direction.DESC, "name"));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(staff), PageRequest.of(2, 7, Sort.by(Sort.Direction.DESC, "canonicalName")), 1));

        var result = adapter.search(true, "jo", requested);

        assertThat(result.getContent()).containsExactly(staff);
        verify(repository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                pageable.getPageNumber() == 2 && pageable.getPageSize() == 7
                        && pageable.getOffset() == 14
                        && pageable.getSort().getOrderFor("canonicalName") != null));
    }

    @Test
    void searchPreservesRosterOffsetWhenOwnerShiftsSecondPage() {
        Pageable requested = new OffsetPageableForTest(1, 2, 1, Sort.by("name"));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 2)));

        adapter.searchExcludingUserId(true, null, 7L, requested);

        verify(repository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                pageable.getPageNumber() == 1 && pageable.getPageSize() == 2 && pageable.getOffset() == 1
                        && pageable.getSort().getOrderFor("canonicalName") != null));
    }

    @Test
    void offsetPageableImplementsNavigationForRosterPages() {
        Pageable requested = new OffsetPageableForTest(2, 5, 7, Sort.by("name"));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        adapter.searchExcludingUserId(true, null, 7L, requested);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        Pageable actual = captor.getValue();
        assertThat(actual.next().getPageNumber()).isEqualTo(3);
        assertThat(actual.next().getOffset()).isEqualTo(12);
        assertThat(actual.previousOrFirst().getPageNumber()).isEqualTo(1);
        assertThat(actual.previousOrFirst().getOffset()).isEqualTo(2);
        assertThat(actual.first().getOffset()).isZero();
        assertThat(actual.withPage(4).getOffset()).isEqualTo(20);
        assertThat(actual.hasPrevious()).isTrue();

        Pageable firstPage = new OffsetPageableForTest(0, 5, 0, Sort.by("name"));
        adapter.searchExcludingUserId(true, null, 7L, firstPage);
        ArgumentCaptor<Pageable> pages = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(2)).findAll(any(Specification.class), pages.capture());
        assertThat(pages.getAllValues().get(1).previousOrFirst().getPageNumber()).isZero();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void searchSpecificationBuildsOwnerExclusionAndSearchPredicates() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        adapter.searchExcludingUserId(true, " Teacher ", 7L,
                PageRequest.of(0, 10, Sort.by("name")));
        adapter.searchExcludingUserId(null, "   ", null, PageRequest.of(0, 10));
        adapter.searchExcludingUserId(null, null, null, Pageable.unpaged());

        ArgumentCaptor<Specification<Staff>> specs = ArgumentCaptor.forClass((Class) Specification.class);
        verify(repository, times(3)).findAll(specs.capture(), any(Pageable.class));
        Root<Staff> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder builder = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        for (Specification<Staff> spec : specs.getAllValues()) {
            Predicate predicate = spec.toPredicate(root, query, builder);
            assertThat(predicate).isNotNull();
        }
    }

    @Test
    void preservesEmptyAndFailureOutcomes() {
        when(repository.findByUserId(2L)).thenReturn(Optional.empty());
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(repository.countByIsActive(false)).thenThrow(failure);

        assertThat(adapter.findByUserId(2L)).isEmpty();
        assertThatThrownBy(() -> adapter.countByIsActive(false)).isSameAs(failure);
    }

    private record OffsetPageableForTest(int pageNumber, int pageSize, long offset, Sort sort) implements Pageable {
        @Override public boolean isPaged() { return true; }
        @Override public boolean isUnpaged() { return false; }
        @Override public int getPageNumber() { return pageNumber; }
        @Override public int getPageSize() { return pageSize; }
        @Override public long getOffset() { return offset; }
        @Override public Sort getSort() { return sort; }
        @Override public Pageable next() { return new OffsetPageableForTest(pageNumber + 1, pageSize, offset + pageSize, sort); }
        @Override public Pageable previousOrFirst() {
            return hasPrevious()
                    ? new OffsetPageableForTest(pageNumber - 1, pageSize, Math.max(0, offset - pageSize), sort)
                    : first();
        }
        @Override public Pageable first() { return new OffsetPageableForTest(0, pageSize, 0, sort); }
        @Override public Pageable withPage(int nextPage) {
            return new OffsetPageableForTest(nextPage, pageSize, (long) nextPage * pageSize, sort);
        }
        @Override public boolean hasPrevious() { return pageNumber > 0; }
    }
}
