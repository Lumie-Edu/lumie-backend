package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.exception.AuthErrors.TenantCreationFailedException;
import com.lumie.auth.application.exception.AuthErrors.UserLoginIdAlreadyExistsException;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.billing.api.BillingService;
import com.lumie.exam.api.ExamService;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.CreateTenantResult;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner registration creates the tenant, its required billing and exam
 * defaults, and the owner identity in one database transaction, then returns
 * the owner's initial session. Redis session state is compensated on rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRegistrationService {

    private final TenantService tenantService;
    private final BillingService billingService;
    private final ExamService examService;
    private final UserLookupPort userLookupPort;
    private final UserDirectoryPort userDirectoryPort;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionHelper loginSessionHelper;

    @Transactional
    public LoginResponse registerOwner(OwnerRegisterRequest request, SessionMetadata metadata) {
        log.info("Owner registration attempt for userLoginId: {}, institute: {}", request.userLoginId(), request.instituteName());

        if (userDirectoryPort.findByUserLoginId(request.userLoginId()).isPresent()) {
            throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
        }

        CreateTenantResult tenantResult = tenantService.createTenant(request.instituteName(), request.subjects());
        if (!tenantResult.success()) {
            throw new TenantCreationFailedException("Failed to create tenant: " + tenantResult.message());
        }

        log.info("Tenant created: slug={}, tenantId={}", tenantResult.tenantSlug(), tenantResult.tenantId());
        TenantContextHolder.withinContext(tenantResult.tenantSlug(), tenantResult.tenantId(), () -> {
            billingService.createTrialSubscription(tenantResult.tenantId(), tenantResult.tenantSlug());
            examService.seedTenantQuestionTypeDefaults(
                    tenantResult.tenantSlug(), tenantResult.tenantId(), request.subjects());
        });

        String passwordHash = passwordEncoder.encode(request.password());

        UserData user = TenantContextHolder.withinContext(tenantResult.tenantSlug(), tenantResult.tenantId(), () ->
                userLookupPort.createUser(
                        request.userLoginId(), request.name(), request.phone(), passwordHash, Role.OWNER));

        // Mirror a slim row into public.owner_directory so root-entry login can
        // resolve this owner's tenant from the OWNER login_id.
        userDirectoryPort.createDirectoryEntry(
                user.id(), request.userLoginId(), tenantResult.tenantId(), null);

        var session = loginSessionHelper.create(user, tenantResult.tenantSlug(), tenantResult.tenantId(), metadata);
        log.info("Owner registration successful: userId={}, tenantSlug={}", user.id(), tenantResult.tenantSlug());
        return LoginResponse.of(session, user.toUserResponse(tenantResult.tenantSlug(), null));
    }
}
