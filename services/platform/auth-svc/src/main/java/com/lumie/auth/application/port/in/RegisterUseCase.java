package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;

public interface RegisterUseCase {
    /**
     * Registers a new user and returns tokens.
     *
     * @param tenantSlug the tenant slug from X-Tenant-Slug header
     * @param request the registration request containing email, password, and name
     * @return login response with tokens and user info
     */
    LoginResponse register(String tenantSlug, RegisterRequest request);
}
