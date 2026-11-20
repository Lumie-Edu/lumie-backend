package com.lumie.file.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.dto.request.CreateUploadSessionRequest;
import com.lumie.file.application.dto.request.UploadableEntityType;
import com.lumie.file.application.event.UploadSessionCleanupDueEvent;
import com.lumie.file.application.port.out.FileUploadSessionPersistencePort;
import com.lumie.file.domain.vo.EntityType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadSessionReservationServiceTest {

    @Mock private FileUploadSessionPersistencePort sessionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private UploadSessionReservationService reservationService;

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenant("file-test");
        TenantContextHolder.setTenantId(42L);
        UserContextHolder.setUserId(10L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void storesTheDomainEntityTypeForAnUploadableType() {
        UUID sessionId = UUID.randomUUID();
        Instant uploadExpiry = Instant.now().plusSeconds(60);
        Instant sessionExpiry = uploadExpiry.plusSeconds(300);
        var request = new CreateUploadSessionRequest(
                UploadableEntityType.QNA, "question.pdf", "application/pdf", 128L);
        given(sessionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        var session = reservationService.reserve(
                sessionId, "file-test/qna/question.pdf", request, uploadExpiry, sessionExpiry);

        assertThat(session.getEntityType()).isEqualTo(EntityType.QNA);
        verify(eventPublisher).publishEvent(any(UploadSessionCleanupDueEvent.class));
    }
}
