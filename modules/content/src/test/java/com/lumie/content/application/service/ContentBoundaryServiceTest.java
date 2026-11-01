package com.lumie.content.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.application.port.out.QnaBoardPersistencePort;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.staff.api.StaffService;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentBoundaryServiceTest {

    @Mock private QnaBoardPersistencePort qnaBoardPersistence;
    @Mock private StudentService studentService;
    @Mock private StaffService staffService;
    @InjectMocks private QnaQueryService queryService;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void searchOrchestrationPassesTenantScopedStudentIdsToPersistencePort() {
        TenantContextHolder.setTenant("tenant-a");
        UserContextHolder.setUserRole(Role.OWNER);
        var pageable = PageRequest.of(0, 10);
        given(studentService.searchUserIds("tenant-a", "algebra")).willReturn(List.of(41L));
        given(qnaBoardPersistence.search("algebra", List.of(41L), null, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = queryService.listQnas("algebra", null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(studentService).searchUserIds("tenant-a", "algebra");
        verify(qnaBoardPersistence).search("algebra", List.of(41L), null, pageable);
    }
}
