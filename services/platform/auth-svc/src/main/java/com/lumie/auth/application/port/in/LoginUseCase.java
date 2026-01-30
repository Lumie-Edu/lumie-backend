package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.response.LoginResponse;

public interface LoginUseCase {
    /**
     * Authenticates a user and returns tokens.
     *
     * @param tenantSlug the tenant slug from X-Tenant-Slug header
     * @param request the login request containing email and password
     * @return login response with tokens and user info
     */
    LoginResponse login(String tenantSlug, LoginRequest request);
}
