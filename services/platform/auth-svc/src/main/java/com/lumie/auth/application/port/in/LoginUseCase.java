package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.response.LoginResponse;

public interface LoginUseCase {
    /**
     * Authenticates a user and returns tokens.
     * Tenant is determined from the user's record in public.users table.
     *
     * @param request the login request containing userLoginId and password
     * @return login response with tokens and user info
     */
    LoginResponse login(LoginRequest request);
}
