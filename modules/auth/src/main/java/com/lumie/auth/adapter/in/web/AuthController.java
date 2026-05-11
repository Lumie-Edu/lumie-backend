package com.lumie.auth.adapter.in.web;

import com.lumie.auth.application.dto.request.ChangePasswordRequest;
import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.RefreshTokenRequest;
import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.request.UpdateAvatarRequest;
import com.lumie.auth.application.dto.request.UpdateProfileRequest;
import com.lumie.auth.application.dto.response.AuthResponse;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.dto.response.SessionResponse;
import com.lumie.auth.application.dto.response.TokenResponse;
import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.application.service.AuthRegistrationService;
import com.lumie.auth.application.service.AuthSessionService;
import com.lumie.auth.application.service.AvatarService;
import com.lumie.auth.application.service.UserProfileService;
import com.lumie.auth.infrastructure.util.SessionMetadataExtractor;
import com.lumie.auth.adapter.out.config.CookieConfig;
import com.lumie.auth.infrastructure.util.CookieUtils;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthRegistrationService authRegistrationService;
    private final AuthSessionService authSessionService;
    private final AuthQueryService authQueryService;
    private final UserProfileService userProfileService;
    private final AvatarService avatarService;
    private final CookieUtils cookieUtils;
    private final CookieConfig cookieConfig;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authQueryService.getMe(userId, tenantSlug));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userProfileService.updateProfile(userId, tenantSlug, request));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) {
            return ResponseEntity.status(401).build();
        }
        userProfileService.changePassword(userId, tenantSlug, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<Void> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        avatarService.updateAvatar(userId, request.avatarSeed());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions() {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        String currentSid = UserContextHolder.getCurrentSid();
        if (userId == null || tenantSlug == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authQueryService.getSessions(userId, tenantSlug, currentSid));
    }

    @DeleteMapping("/me/sessions/{sid}")
    public ResponseEntity<Void> revokeSession(@PathVariable String sid) {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        String currentSid = UserContextHolder.getCurrentSid();
        if (userId == null || tenantSlug == null) {
            return ResponseEntity.status(401).build();
        }
        if (sid.equals(currentSid)) {
            return ResponseEntity.badRequest().build();
        }
        authSessionService.revokeSession(userId, tenantSlug, sid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/sessions/revoke-others")
    public ResponseEntity<Void> revokeOtherSessions() {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        String currentSid = UserContextHolder.getCurrentSid();
        if (userId == null || tenantSlug == null) {
            return ResponseEntity.status(401).build();
        }
        authSessionService.revokeOtherSessions(userId, tenantSlug, currentSid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        var metadata = SessionMetadataExtractor.extract(httpRequest);
        LoginResponse response = authRegistrationService.register(request, metadata);
        return buildAuthResponse(response, 201);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponse> registerOwner(
            @Valid @RequestBody OwnerRegisterRequest request, HttpServletRequest httpRequest) {
        var metadata = SessionMetadataExtractor.extract(httpRequest);
        LoginResponse response = authRegistrationService.registerOwner(request, metadata);
        return buildAuthResponse(response, 201);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        var metadata = SessionMetadataExtractor.extract(httpRequest);
        LoginResponse response = authSessionService.login(request, metadata);
        return buildAuthResponse(response, 200);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "lumie_refresh_token", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null && request != null) {
            refreshToken = request.refreshToken();
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        TokenResponse response = authSessionService.refresh(
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
            authSessionService.logout(token);
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
            authSessionService.logoutAll(token);
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
