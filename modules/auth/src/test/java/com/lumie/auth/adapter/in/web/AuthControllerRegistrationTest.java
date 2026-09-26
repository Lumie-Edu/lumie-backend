package com.lumie.auth.adapter.in.web;

import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.application.service.AuthRegistrationService;
import com.lumie.auth.application.service.AuthSessionService;
import com.lumie.auth.application.service.AvatarService;
import com.lumie.auth.application.service.UserProfileService;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthControllerRegistrationTest {

    @Mock private AuthRegistrationService authRegistrationService;
    @Mock private AuthSessionService authSessionService;
    @Mock private AuthQueryService authQueryService;
    @Mock private UserProfileService userProfileService;
    @Mock private AvatarService avatarService;
    @Mock private CookieUtils cookieUtils;
    @Mock private HttpServletRequest request;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void ownerRegistrationIsUnavailableFromATenantHost() {
        TenantContextHolder.setTenant("inst-joossameng");

        var response = controller().registerOwner(null, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verifyNoInteractions(authRegistrationService);
    }

    private AuthController controller() {
        return new AuthController(
                authRegistrationService,
                authSessionService,
                authQueryService,
                userProfileService,
                avatarService,
                cookieUtils,
                new SessionConfig());
    }
}
