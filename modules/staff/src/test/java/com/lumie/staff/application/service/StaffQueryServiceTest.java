package com.lumie.staff.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.staff.application.dto.request.StaffFilterRequest;
import com.lumie.staff.application.port.out.StaffPermissionPersistencePort;
import com.lumie.staff.application.port.out.StaffPersistencePort;
import com.lumie.staff.domain.entity.Staff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StaffQueryServiceTest {

    @Mock private StaffPersistencePort staffRepository;
    @Mock private StaffPermissionPersistencePort staffPermissionRepository;
    @Mock private AuthService authService;
    @InjectMocks private StaffQueryService service;

    @Test
    void unfilteredListUsesRepositorySearchSoNameSortIsMapped() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
        given(authService.getTenantOwner()).willReturn(Optional.empty());
        given(staffRepository.searchExcludingUserId(null, null, null, pageable)).willReturn(Page.empty(pageable));

        assertThat(service.getAllStaff(null, pageable)).isEmpty();

        verify(staffRepository).searchExcludingUserId(null, null, null, pageable);
        verify(authService).getTenantOwner();
    }

    @Test
    void sizeOneKeepsOwnerOnFirstPageAndStartsEmployeesAtFirstOffsetOnSecondPage() {
        var owner = owner();
        Staff firstEmployee = Staff.create(20L, "teacher1", "Teacher One", null, null, null);
        given(authService.getTenantOwner()).willReturn(Optional.of(owner));
        given(staffRepository.searchExcludingUserId(eq(true), eq(null), eq(7L),
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 1
                        && pageable.getOffset() == 0)))
                .willReturn(new PageImpl<>(List.of(firstEmployee), PageRequest.of(0, 1), 2));
        given(staffRepository.searchExcludingUserId(eq(true), eq(null), eq(7L),
                argThat(pageable -> pageable.getPageNumber() == 1 && pageable.getPageSize() == 1
                        && pageable.getOffset() == 0)))
                .willReturn(new PageImpl<>(List.of(firstEmployee), PageRequest.of(0, 1), 2));

        var firstPage = service.getAllStaff(new StaffFilterRequest(true, null), PageRequest.of(0, 1));
        var secondPage = service.getAllStaff(new StaffFilterRequest(true, null), PageRequest.of(1, 1));

        assertThat(firstPage.getContent()).singleElement()
                .satisfies(row -> {
                    assertThat(row.role()).isEqualTo("OWNER");
                    assertThat(row.userId()).isEqualTo(7L);
                });
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getContent()).singleElement()
                .satisfies(row -> assertThat(row.userId()).isEqualTo(20L));
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
    }

    @Test
    void sizeTwoCombinesOwnerWithOneEmployeeAndSecondPageContinuesAtOffsetOne() {
        var owner = owner();
        Staff firstEmployee = Staff.create(20L, "teacher1", "Teacher One", null, null, null);
        Staff secondEmployee = Staff.create(21L, "teacher2", "Teacher Two", null, null, null);
        Staff thirdEmployee = Staff.create(22L, "teacher3", "Teacher Three", null, null, null);
        given(authService.getTenantOwner()).willReturn(Optional.of(owner));
        given(staffRepository.searchExcludingUserId(eq(true), eq(null), eq(7L),
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 1
                        && pageable.getOffset() == 0)))
                .willReturn(new PageImpl<>(List.of(firstEmployee), PageRequest.of(0, 1), 3));
        given(staffRepository.searchExcludingUserId(eq(true), eq(null), eq(7L),
                argThat(pageable -> pageable.getPageNumber() == 1 && pageable.getPageSize() == 2
                        && pageable.getOffset() == 1)))
                .willReturn(new PageImpl<>(List.of(secondEmployee, thirdEmployee), PageRequest.of(0, 2), 3));

        var firstPage = service.getAllStaff(new StaffFilterRequest(true, null), PageRequest.of(0, 2));
        var secondPage = service.getAllStaff(new StaffFilterRequest(true, null), PageRequest.of(1, 2));

        assertThat(firstPage.getContent()).extracting("userId").containsExactly(7L, 20L);
        assertThat(secondPage.getContent()).extracting("userId").containsExactly(21L, 22L);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(secondPage.getTotalElements()).isEqualTo(4);
    }

    @Test
    void ownerSearchAndActiveFilterControlWhetherVirtualOwnerIsIncluded() {
        var owner = owner();
        Staff employee = Staff.create(20L, "ownerid-teacher", "Matching Teacher", null, null, null);
        given(authService.getTenantOwner()).willReturn(Optional.of(owner));
        given(staffRepository.searchExcludingUserId(eq(true), eq("ownerid"), eq(7L),
                argThat(pageable -> pageable.getPageSize() == 1 && pageable.getOffset() == 0)))
                .willReturn(new PageImpl<>(List.of(employee), PageRequest.of(0, 1), 1));
        given(staffRepository.searchExcludingUserId(eq(false), eq("ownerid"), eq(7L),
                argThat(pageable -> pageable.getPageSize() == 2 && pageable.getOffset() == 0)))
                .willReturn(Page.empty(PageRequest.of(0, 2)));

        var ownerSearch = service.getAllStaff(
                new StaffFilterRequest(true, "ownerid"), PageRequest.of(0, 2));
        var inactiveSearch = service.getAllStaff(
                new StaffFilterRequest(false, "ownerid"), PageRequest.of(0, 2));

        assertThat(ownerSearch.getContent()).extracting("userId").containsExactly(7L, 20L);
        assertThat(ownerSearch.getTotalElements()).isEqualTo(2);
        assertThat(inactiveSearch.getContent()).isEmpty();
        assertThat(inactiveSearch.getTotalElements()).isZero();
    }

    @Test
    void ownerShiftedPageableSupportsPageNavigationAcrossFirstAndLaterPages() {
        given(authService.getTenantOwner()).willReturn(Optional.of(owner()));
        given(authService.getAvatarSeeds(List.of(7L))).willReturn(Map.of(7L, "seed"));
        given(staffRepository.searchExcludingUserId(eq(null), eq(null), eq(7L), any(Pageable.class)))
                .willReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        service.getAllStaff(null, PageRequest.of(0, 3));
        service.getAllStaff(null, PageRequest.of(1, 3));

        verify(staffRepository, org.mockito.Mockito.times(2))
                .searchExcludingUserId(eq(null), eq(null), eq(7L), pageableCaptor.capture());
        Pageable firstPage = pageableCaptor.getAllValues().get(0);
        Pageable secondPage = pageableCaptor.getAllValues().get(1);
        assertThat(firstPage.getPageSize()).isEqualTo(2);
        assertThat(firstPage.isPaged()).isTrue();
        assertThat(firstPage.isUnpaged()).isFalse();
        assertThat(firstPage.getSort().isUnsorted()).isTrue();
        assertThat(firstPage.getOffset()).isZero();
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.previousOrFirst().getPageNumber()).isZero();
        assertThat(secondPage.getPageSize()).isEqualTo(3);
        assertThat(secondPage.getOffset()).isEqualTo(2);
        assertThat(secondPage.hasPrevious()).isTrue();
        assertThat(secondPage.next().getOffset()).isEqualTo(5);
        assertThat(secondPage.previousOrFirst().getOffset()).isZero();
        assertThat(secondPage.first().getOffset()).isZero();
        assertThat(secondPage.withPage(4).getOffset()).isEqualTo(12);
    }

    @Test
    void ownerVisibilityMatchesNameEmailAndBlankSearch() {
        given(authService.getTenantOwner()).willReturn(Optional.of(owner()));
        given(authService.getAvatarSeeds(List.of(7L))).willReturn(Map.of(7L, "seed"));
        given(staffRepository.searchExcludingUserId(any(), any(), eq(7L), any(Pageable.class)))
                .willReturn(Page.empty());

        var byName = service.getAllStaff(new StaffFilterRequest(null, "Academy"), PageRequest.of(0, 10));
        var byEmail = service.getAllStaff(new StaffFilterRequest(true, "owner@example.com"), PageRequest.of(0, 10));
        var blankSearch = service.getAllStaff(new StaffFilterRequest(null, "   "), PageRequest.of(0, 10));

        assertThat(byName.getContent()).singleElement().satisfies(row -> assertThat(row.role()).isEqualTo("OWNER"));
        assertThat(byEmail.getContent()).singleElement().satisfies(row -> assertThat(row.role()).isEqualTo("OWNER"));
        assertThat(blankSearch.getContent()).singleElement()
                .satisfies(row -> assertThat(row.role()).isEqualTo("OWNER"));
    }

    private AuthService.TenantOwnerData owner() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        return new AuthService.TenantOwnerData(
                7L, "ownerid", "Academy Owner", "01012345678", "owner@example.com", null,
                createdAt, createdAt);
    }
}
