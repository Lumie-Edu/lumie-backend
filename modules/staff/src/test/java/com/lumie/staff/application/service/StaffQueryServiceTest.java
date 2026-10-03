package com.lumie.staff.application.service;

import com.lumie.auth.api.AuthService;
import com.lumie.staff.domain.repository.StaffPermissionRepository;
import com.lumie.staff.domain.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StaffQueryServiceTest {

    @Mock private StaffRepository staffRepository;
    @Mock private StaffPermissionRepository staffPermissionRepository;
    @Mock private AuthService authService;
    @InjectMocks private StaffQueryService service;

    @Test
    void unfilteredListUsesRepositorySearchSoNameSortIsMapped() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
        given(staffRepository.search(null, null, pageable)).willReturn(Page.empty(pageable));

        assertThat(service.getAllStaff(null, pageable)).isEmpty();

        verify(staffRepository).search(null, null, pageable);
        verifyNoInteractions(authService);
    }
}
