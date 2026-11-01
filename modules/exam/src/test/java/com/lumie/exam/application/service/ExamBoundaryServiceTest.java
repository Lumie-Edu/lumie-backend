package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.OmrPresignRequest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ExamBoundaryServiceTest {

    @Test
    void storageCallsStartAfterTheServiceTransactionHasEnded() throws Exception {
        assertNotTransactional(ResultCommandService.class.getMethod(
                "generatePresignedUrls", Long.class, OmrPresignRequest.class));
        assertNotTransactional(ResultCommandService.class.getMethod(
                "deleteResult", Long.class, Long.class));
        assertNotTransactional(OmrSubmissionService.class.getMethod(
                "getImage", Long.class, Long.class));
        assertThat(ResultCommandService.Tx.class.getMethod("deleteResult", Long.class, Long.class)
                .getAnnotation(Transactional.class)).isNotNull();
        assertThat(OmrSubmissionService.Tx.class.getMethod("loadImageKey", Long.class, Long.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }

    private static void assertNotTransactional(Method method) {
        assertThat(method.getAnnotation(Transactional.class))
                .as("%s must complete persistence work before outbound storage I/O", method)
                .isNull();
    }
}
