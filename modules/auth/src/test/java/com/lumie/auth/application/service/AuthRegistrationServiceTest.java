package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.exception.AuthErrors.TenantCreationFailedException;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.billing.api.BillingService;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamService;
import com.lumie.tenant.api.TenantService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthRegistrationServiceTest {

    private static final String TENANT_SLUG = "academy-a";
    private static final Long TENANT_ID = 22L;
    private static final List<String> SUBJECTS = List.of("KOREAN", "ENGLISH");

    @Mock private TenantService tenantService;
    @Mock private BillingService billingService;
    @Mock private ExamService examService;
    @Mock private UserLookupPort userLookupPort;
    @Mock private UserDirectoryPort userDirectoryPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginSessionHelper loginSessionHelper;
    @InjectMocks private AuthRegistrationService service;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void registerOwnerSynchronouslyCreatesTenantDefaultsAndOwnerSession() {
        OwnerRegisterRequest request = request();
        UserData owner = new UserData(8L, "owner-login", "Academy Owner", "01012345678", null,
                "hash", Role.OWNER, TENANT_ID, true, "seed");
        AuthSession session = new AuthSession("session-id", 8L, "Academy Owner", TENANT_SLUG, TENANT_ID,
                Role.OWNER, "Browser", DeviceCategory.PC, Instant.EPOCH, Instant.EPOCH.plusSeconds(600));
        given(userDirectoryPort.findByUserLoginId("owner-login")).willReturn(Optional.empty());
        given(tenantService.createTenant("Academy", SUBJECTS))
                .willReturn(new TenantService.CreateTenantResult(true, "created", TENANT_ID, TENANT_SLUG));
        given(passwordEncoder.encode("StrongPass1!")).willReturn("hash");
        given(userLookupPort.createUser("owner-login", "Academy Owner", "01012345678", "hash", Role.OWNER))
                .willAnswer(invocation -> {
                    assertThat(TenantContextHolder.getTenant()).isEqualTo(TENANT_SLUG);
                    assertThat(TenantContextHolder.getTenantId()).isEqualTo(TENANT_ID);
                    return owner;
                });
        given(loginSessionHelper.create(owner, TENANT_SLUG, TENANT_ID,
                new SessionMetadata("Browser", DeviceCategory.PC))).willReturn(session);

        LoginResponse response = service.registerOwner(request, new SessionMetadata("Browser", DeviceCategory.PC));

        assertThat(response.sessionId()).isEqualTo("session-id");
        assertThat(response.user().role()).isEqualTo(Role.OWNER);
        assertThat(TenantContextHolder.getTenant()).isNull();
        InOrder order = inOrder(tenantService, billingService, examService, userLookupPort, userDirectoryPort);
        order.verify(tenantService).createTenant("Academy", SUBJECTS);
        order.verify(billingService).createTrialSubscription(TENANT_ID, TENANT_SLUG);
        order.verify(examService).seedTenantQuestionTypeDefaults(TENANT_SLUG, TENANT_ID, SUBJECTS);
        order.verify(userLookupPort).createUser("owner-login", "Academy Owner", "01012345678", "hash", Role.OWNER);
        order.verify(userDirectoryPort).createDirectoryEntry(8L, "owner-login", TENANT_ID, null);
    }

    @Test
    void registerOwnerRejectsDuplicateLoginBeforeProvisioning() {
        given(userDirectoryPort.findByUserLoginId("owner-login"))
                .willReturn(Optional.of(new UserDirectoryPort.DirectoryEntry(8L, "owner-login", TENANT_ID, null)));

        assertThatThrownBy(() -> service.registerOwner(request(), new SessionMetadata(null, null)))
                .isInstanceOf(com.lumie.auth.application.exception.AuthErrors.UserLoginIdAlreadyExistsException.class);

        verify(tenantService, never()).createTenant("Academy", SUBJECTS);
    }

    @Test
    void registerOwnerAbortsWhenTenantCreationFails() {
        given(userDirectoryPort.findByUserLoginId("owner-login")).willReturn(Optional.empty());
        given(tenantService.createTenant("Academy", SUBJECTS))
                .willReturn(new TenantService.CreateTenantResult(false, "unavailable", null, null));

        assertThatThrownBy(() -> service.registerOwner(request(), new SessionMetadata(null, null)))
                .isInstanceOf(TenantCreationFailedException.class);

        verify(billingService, never()).createTrialSubscription(TENANT_ID, TENANT_SLUG);
        verify(examService, never()).seedTenantQuestionTypeDefaults(TENANT_SLUG, TENANT_ID, SUBJECTS);
    }

    private OwnerRegisterRequest request() {
        return OwnerRegisterRequest.builder()
                .userLoginId("owner-login")
                .password("StrongPass1!")
                .name("Academy Owner")
                .phone("01012345678")
                .instituteName("Academy")
                .subjects(SUBJECTS)
                .build();
    }
}
