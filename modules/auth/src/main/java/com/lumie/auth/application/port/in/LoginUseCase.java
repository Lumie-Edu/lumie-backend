package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.domain.vo.SessionMetadata;

public interface LoginUseCase {
    /**
     * Authenticates a user and returns tokens.
     * Tenant is determined from the user's record in public.users table.
     *
     * @param request the login request containing userLoginId and password
     * @param metadata session metadata (device, IP) captured from the HTTP request
     * @return login response with tokens and user info
     */
    LoginResponse login(LoginRequest request, SessionMetadata metadata);
}
