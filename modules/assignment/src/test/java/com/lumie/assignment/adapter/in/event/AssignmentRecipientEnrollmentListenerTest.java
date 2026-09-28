package com.lumie.assignment.adapter.in.event;

import com.lumie.assignment.application.service.AssignmentRecipientService;
import com.lumie.classroom.api.ClassEnrollmentCreatedEvent;
import com.lumie.common.test.TenantContextExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
class AssignmentRecipientEnrollmentListenerTest {

    @Mock private AssignmentRecipientService recipientService;
    @InjectMocks private AssignmentRecipientEnrollmentListener listener;

    @Test
    void matchingTenantAddsNewEnrolleesToOpenAssignments() {
        listener.onEnrollmentCreated(new ClassEnrollmentCreatedEvent(
                1L, "test-tenant", 10L, List.of(3L, 4L)));

        verify(recipientService).addNewClassEnrollees("test-tenant", 10L, List.of(3L, 4L));
    }

    @Test
    void differentTenantDoesNotChangeRecipients() {
        listener.onEnrollmentCreated(new ClassEnrollmentCreatedEvent(
                999L, "another-tenant", 10L, List.of(3L)));

        verify(recipientService, never()).addNewClassEnrollees("another-tenant", 10L, List.of(3L));
    }
}
