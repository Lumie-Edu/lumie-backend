package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.domain.vo.SessionMetadata;

public interface OwnerRegisterUseCase {

    /**
     * Registers a new owner with a new tenant.
     * Creates the tenant, provisions the schema, and creates the owner user.
     *
     * @param request the owner registration request
     * @param metadata session metadata (device, IP) captured from the HTTP request
     * @return login response with tokens and user info
     */
    LoginResponse registerOwner(OwnerRegisterRequest request, SessionMetadata metadata);
}
