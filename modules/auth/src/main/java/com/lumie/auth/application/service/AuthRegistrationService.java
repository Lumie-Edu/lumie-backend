package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.exception.AuthErrors.TenantCreationFailedException;
import com.lumie.auth.application.exception.AuthErrors.UserLoginIdAlreadyExistsException;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.api.OwnerRegisteredEvent;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.CreateTenantResult;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Owner registration provisions a tenant and issues the owner's initial
 * session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRegistrationService {

    private final TenantService tenantService;
    private final UserLookupPort userLookupPort;
    private final UserDirectoryPort userDirectoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionHelper loginSessionHelper;

    public LoginResponse registerOwner(OwnerRegisterRequest request, SessionMetadata metadata) {
        log.info("Owner registration attempt for userLoginId: {}, institute: {}", request.userLoginId(), request.instituteName());

        if (userDirectoryPort.findByUserLoginId(request.userLoginId()).isPresent()) {
            throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
        }

        CreateTenantResult tenantResult = tenantService.createTenant(request.instituteName());
        if (!tenantResult.success()) {
            throw new TenantCreationFailedException("Failed to create tenant: " + tenantResult.message());
        }

        log.info("Tenant created: slug={}, tenantId={}", tenantResult.tenantSlug(), tenantResult.tenantId());
        String passwordHash = passwordEncoder.encode(request.password());

        UserData user = TenantContextHolder.withinContext(tenantResult.tenantSlug(), tenantResult.tenantId(), () ->
                userLookupPort.createUser(
                        request.userLoginId(), request.name(), request.phone(), passwordHash, Role.OWNER));

        // Mirror a slim row into public.owner_directory so root-entry login can
        // resolve this owner's tenant from the OWNER login_id.
        userDirectoryPort.createDirectoryEntry(
                user.id(), request.userLoginId(), tenantResult.tenantId(), null);

        // Bootstrap of OWNER admin record runs as an AFTER_COMMIT listener on
        // this event in admin. Breaks the previous auth↔admin cycle
        // (CIRCULAR_MODULE_DEPS_AUDIT Resolution A, pair 4).
        eventPublisher.publishEvent(new OwnerRegisteredEvent(
                user.id(), request.userLoginId(), request.name(), request.phone(), null,
                tenantResult.tenantSlug(), tenantResult.tenantId()));

        var session = loginSessionHelper.create(user, tenantResult.tenantSlug(), tenantResult.tenantId(), metadata);
        log.info("Owner registration successful: userId={}, tenantSlug={}", user.id(), tenantResult.tenantSlug());
        return LoginResponse.of(session, user.toUserResponse(tenantResult.tenantSlug(), null));
    }
}
