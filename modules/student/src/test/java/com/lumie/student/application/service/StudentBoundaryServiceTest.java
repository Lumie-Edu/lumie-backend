package com.lumie.student.application.service;

import com.lumie.student.adapter.in.internal.StudentServiceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class StudentBoundaryServiceTest {

    @Test
    void deletionExternalCallsRunOutsideDatabaseTransaction() throws Exception {
        assertThat(StudentCommandService.class.getMethod("deleteStudent", Long.class)
                .getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
        assertThat(StudentCommandService.class.getMethod("batchDelete", java.util.List.class)
                .getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }

    @Test
    void internalStudentAdapterRemainsReadOnlyAtTheApplicationBoundary() {
        assertThat(StudentServiceAdapter.class.getAnnotation(Transactional.class).readOnly()).isTrue();
    }
}
