package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.exception.AuthErrors.TenantCreationFailedException;
import com.lumie.auth.application.exception.AuthErrors.UserLoginIdAlreadyExistsException;
import com.lumie.auth.application.port.in.OwnerRegisterUseCase;
import com.lumie.auth.application.port.in.RegisterUseCase;
import com.lumie.auth.application.port.out.AdminServicePort;
import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.auth.application.port.out.TenantServicePort.TenantCreationResult;
import com.lumie.auth.application.port.out.TenantServicePort.TenantData;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.application.service.LoginSessionHelper.IssuedTokens;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Registration use cases — student self-register and owner registration
 * (which provisions a brand-new tenant). Both paths end with an immediate
 * login token issuance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRegistrationService implements RegisterUseCase, OwnerRegisterUseCase {

    private final TenantServicePort tenantServicePort;
    private final UserLookupPort userLookupPort;
    private final UserDirectoryPort userDirectoryPort;
    private final AdminServicePort adminServicePort;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionHelper loginSessionHelper;

    @Override
    public LoginResponse register(RegisterRequest request, SessionMetadata metadata) {
        log.info("Registration attempt for userLoginId: {} on tenant: {}", request.userLoginId(), request.tenantSlug());

        TenantData tenant = tenantServicePort.validateTenant(request.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantSlug()));
        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active: " + request.tenantSlug());
        }

        return TenantContextHolder.withinContext(tenant.slug(), tenant.id(), () -> {
            if (userLookupPort.existsByUserLoginId(request.userLoginId())) {
                throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
            }

            String passwordHash = passwordEncoder.encode(request.password());
            UserData user = userLookupPort.createUser(
                    request.userLoginId(), request.name(), request.phone(), passwordHash, Role.STUDENT);

            IssuedTokens issued = loginSessionHelper.issueTokensForLogin(user, request.tenantSlug(), tenant.id(), metadata);
            log.info("Registration successful for user: {} on tenant: {}", user.id(), request.tenantSlug());
            return LoginResponse.of(issued.tokenPair(), user.toUserResponse(request.tenantSlug()));
        });
    }

    @Override
    public LoginResponse registerOwner(OwnerRegisterRequest request, SessionMetadata metadata) {
        log.info("Owner registration attempt for userLoginId: {}, institute: {}", request.userLoginId(), request.instituteName());

        if (userDirectoryPort.findByUserLoginId(request.userLoginId()).isPresent()) {
            throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
        }

        TenantCreationResult tenantResult = tenantServicePort.createTenant(
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

        adminServicePort.createOwnerAdmin(
                tenantResult.tenantSlug(), user.id(),
                request.userLoginId(), request.name(), request.phone(), null);

        IssuedTokens issued = loginSessionHelper.issueTokensForLogin(user, tenantResult.tenantSlug(), tenantResult.tenantId(), metadata);
        log.info("Owner registration successful: userId={}, tenantSlug={}", user.id(), tenantResult.tenantSlug());
        return LoginResponse.of(issued.tokenPair(), user.toUserResponse(tenantResult.tenantSlug()));
    }
}
