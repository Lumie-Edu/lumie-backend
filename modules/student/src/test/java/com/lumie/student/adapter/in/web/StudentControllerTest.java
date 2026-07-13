package com.lumie.student.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.student.application.service.StudentCommandService;
import com.lumie.student.application.service.StudentQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentCommandService studentCommandService;

    @Mock
    private StudentQueryService studentQueryService;

    @Mock
    private IdempotencyService idempotencyService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void bulkImportStudents_rejectsStudentRoleBeforeHashingFile() {
        UserContextHolder.setUserRole(Role.STUDENT);
        StudentController controller = new StudentController(
                studentCommandService,
                studentQueryService,
                idempotencyService
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                "학생 이름,학생 연락처\n".getBytes(UTF_8)
        );

        assertThatThrownBy(() -> controller.bulkImportStudents(
                file,
                "550e8400-e29b-41d4-a716-446655440000"
        ))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));

        verify(studentCommandService, never()).hashBulkImportFile(file);
    }
}
