package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;

public interface RegisterUseCase {
    /**
     * Registers a new user and returns tokens.
     *
     * @param request the registration request containing tenantSlug, userLoginId, password, name, and phone
     * @return login response with tokens and user info
     */
    LoginResponse register(RegisterRequest request);
}
