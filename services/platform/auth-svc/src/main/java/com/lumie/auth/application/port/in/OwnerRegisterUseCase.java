package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;

public interface OwnerRegisterUseCase {

    /**
     * Registers a new owner with a new tenant.
     * Creates the tenant, provisions the schema, and creates the owner user.
     *
     * @param request the owner registration request
     * @return login response with tokens and user info
     */
    LoginResponse registerOwner(OwnerRegisterRequest request);
}
