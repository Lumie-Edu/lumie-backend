package com.lumie.auth.adapter.in.web;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.RefreshTokenRequest;
import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.response.AuthResponse;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.dto.response.TokenResponse;
import com.lumie.auth.application.port.in.LoginUseCase;
import com.lumie.auth.application.port.in.LogoutUseCase;
import com.lumie.auth.application.port.in.OwnerRegisterUseCase;
import com.lumie.auth.application.port.in.RefreshTokenUseCase;
import com.lumie.auth.application.port.in.RegisterUseCase;
import com.lumie.auth.infrastructure.config.CookieConfig;
import com.lumie.auth.infrastructure.util.CookieUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final OwnerRegisterUseCase ownerRegisterUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final CookieUtils cookieUtils;
    private final CookieConfig cookieConfig;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = registerUseCase.register(request);
        return buildAuthResponse(response, 201);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponse> registerOwner(@Valid @RequestBody OwnerRegisterRequest request) {
        LoginResponse response = ownerRegisterUseCase.registerOwner(request);
        return buildAuthResponse(response, 201);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginUseCase.login(request);
        return buildAuthResponse(response, 200);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "lumie_refresh_token", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        // Support both cookie-based and body-based refresh token
        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null && request != null) {
            refreshToken = request.refreshToken();
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        TokenResponse response = refreshTokenUseCase.refresh(
                RefreshTokenRequest.builder().refreshToken(refreshToken).build()
        );
        return buildTokenRefreshResponse(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "lumie_access_token", required = false) String accessTokenFromCookie,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = accessTokenFromCookie;
        if (token == null && authHeader != null) {
            token = extractToken(authHeader);
        }

        if (token != null) {
            logoutUseCase.logout(token);
        }

        return buildLogoutResponse();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            @CookieValue(name = "lumie_access_token", required = false) String accessTokenFromCookie,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = accessTokenFromCookie;
        if (token == null && authHeader != null) {
            token = extractToken(authHeader);
        }

        if (token != null) {
            logoutUseCase.logoutAll(token);
        }

        return buildLogoutResponse();
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(LoginResponse response, int status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createAccessTokenCookie(response.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createRefreshTokenCookie(response.refreshToken()).toString())
                .body(AuthResponse.from(response));
    }

    private ResponseEntity<AuthResponse> buildTokenRefreshResponse(TokenResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createAccessTokenCookie(response.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createRefreshTokenCookie(response.refreshToken()).toString())
                .body(AuthResponse.from(response));
    }

    private ResponseEntity<Void> buildLogoutResponse() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createExpiredAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createExpiredRefreshTokenCookie().toString())
                .build();
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
