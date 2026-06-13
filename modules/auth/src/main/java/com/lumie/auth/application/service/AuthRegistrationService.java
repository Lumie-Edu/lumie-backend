package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.exception.AuthErrors.TenantCreationFailedException;
import com.lumie.auth.application.exception.AuthErrors.TenantInactiveException;
import com.lumie.auth.application.exception.AuthErrors.UserLoginIdAlreadyExistsException;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.api.OwnerRegisteredEvent;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.CreateTenantResult;
import com.lumie.tenant.api.TenantService.TenantData;
import com.lumie.auth.application.service.LoginSessionHelper.IssuedTokens;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration use cases — student self-register and owner registration
 * (which provisions a brand-new tenant). Both paths end with an immediate
 * login token issuance.
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

    @Transactional
    public LoginResponse register(RegisterRequest request, SessionMetadata metadata) {
        log.info("Registration attempt for userLoginId: {} on tenant: {}", request.userLoginId(), request.tenantSlug());

        TenantData tenant = tenantService.getTenantBySlug(request.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantSlug()));
        if (!tenant.isActive()) {
            throw new TenantInactiveException("Tenant is not active: " + request.tenantSlug());
        }

        return TenantContextHolder.withinContext(tenant.slug(), tenant.id(), () -> {
            if (userLookupPort.existsByUserLoginId(request.userLoginId())) {
                throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
            }

            String passwordHash = passwordEncoder.encode(request.password());
            UserData user = userLookupPort.createUser(
                    request.userLoginId(), request.name(), request.phone(), passwordHash, Role.STUDENT);

            IssuedTokens issued = loginSessionHelper.issueTokensForLogin(user, request.tenantSlug(), tenant.id(), metadata);
            
            eventPublisher.publishEvent(new com.lumie.auth.api.StudentSelfRegisteredEvent(
                    user.id(), request.userLoginId(), request.name(), request.phone(),
                    request.studentHighschool(), request.studentBirthYear(), request.parentPhone(),
                    request.tenantSlug(), tenant.id()
            ));
            
            log.info("Registration successful for user: {} on tenant: {}", user.id(), request.tenantSlug());
            return LoginResponse.of(issued.tokenPair(), user.toUserResponse(
                    request.tenantSlug(),
                    tenant.onboardingCompletedAt()
            ));
        });
    }

    public LoginResponse registerOwner(OwnerRegisterRequest request, SessionMetadata metadata) {
        log.info("Owner registration attempt for userLoginId: {}, institute: {}", request.userLoginId(), request.instituteName());

        if (userDirectoryPort.findByUserLoginId(request.userLoginId()).isPresent()) {
            throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
        }

        CreateTenantResult tenantResult = tenantService.createTenant(
                request.instituteName(), request.businessRegistrationNumber(),
                request.userLoginId(), request.name());
        if (!tenantResult.success()) {
            throw new TenantCreationFailedException("Failed to create tenant: " + tenantResult.message());
        }

        log.info("Tenant created: slug={}, tenantId={}", tenantResult.tenantSlug(), tenantResult.tenantId());
        String passwordHash = passwordEncoder.encode(request.password());

        UserData user = TenantContextHolder.withinContext(tenantResult.tenantSlug(), tenantResult.tenantId(), () ->
                userLookupPort.createUser(
                        request.userLoginId(), request.name(), request.phone(), passwordHash, Role.OWNER));

        // Mirror a slim row into the public.users directory so root-entry login
        // can resolve this owner's tenant from the OWNER login_id.
        userDirectoryPort.createDirectoryEntry(
                user.id(), request.userLoginId(), tenantResult.tenantId(), null);

        // Bootstrap of OWNER admin record runs as an AFTER_COMMIT listener on
        // this event in admin. Breaks the previous auth↔admin cycle
        // (CIRCULAR_MODULE_DEPS_AUDIT Resolution A, pair 4).
        eventPublisher.publishEvent(new OwnerRegisteredEvent(
                user.id(), request.userLoginId(), request.name(), request.phone(), null,
                tenantResult.tenantSlug(), tenantResult.tenantId()));

        IssuedTokens issued = loginSessionHelper.issueTokensForLogin(user, tenantResult.tenantSlug(), tenantResult.tenantId(), metadata);
        log.info("Owner registration successful: userId={}, tenantSlug={}", user.id(), tenantResult.tenantSlug());
        return LoginResponse.of(issued.tokenPair(), user.toUserResponse(tenantResult.tenantSlug(), null));
    }
}
