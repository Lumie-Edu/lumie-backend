package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.Staff;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

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
