package com.lumie.auth.adapter.in.web;

import com.lumie.auth.application.dto.request.ChangePasswordRequest;
import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.UpdateAvatarRequest;
import com.lumie.auth.application.dto.request.UpdateProfileRequest;
import com.lumie.auth.application.dto.response.AuthResponse;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.dto.response.SessionResponse;
import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.application.service.AuthRegistrationService;
import com.lumie.auth.application.service.AuthSessionService;
import com.lumie.auth.application.service.AvatarService;
import com.lumie.auth.application.service.UserProfileService;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final SessionConfig sessionConfig;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(authQueryService.getMe(userId, tenantSlug));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userProfileService.updateProfile(userId, tenantSlug, request));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) return ResponseEntity.status(401).build();
        userProfileService.changePassword(userId, tenantSlug, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<Void> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) return ResponseEntity.status(401).build();
        avatarService.updateAvatar(userId, request.avatarSeed());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions() {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(authQueryService.getSessions(userId, tenantSlug, UserContextHolder.getCurrentSid()));
    }

    @DeleteMapping("/me/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable String sessionId) {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) return ResponseEntity.status(401).build();
        if (sessionId.equals(UserContextHolder.getCurrentSid())) return ResponseEntity.badRequest().build();
        authSessionService.revokeSession(userId, tenantSlug, sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/sessions")
    public ResponseEntity<Void> revokeOtherSessions() {
        Long userId = UserContextHolder.getUserId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (userId == null || tenantSlug == null) return ResponseEntity.status(401).build();
        authSessionService.revokeOtherSessions(userId, tenantSlug, UserContextHolder.getCurrentSid());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponse> registerOwner(
            @Valid @RequestBody OwnerRegisterRequest request, HttpServletRequest httpRequest) {
        // Owner signup is a Lumie-product entry point. A tenant host must
        // never provision an unrelated tenant, even if a caller bypasses the
        // public route and calls the API directly.
        if (TenantContextHolder.getTenant() != null) {
            return ResponseEntity.notFound().build();
        }
        return buildAuthResponse(authRegistrationService.registerOwner(request, SessionMetadataExtractor.extract(httpRequest)), 201);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return buildAuthResponse(authSessionService.login(request, SessionMetadataExtractor.extract(httpRequest)), 200);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String sessionId = sessionId(request);
        if (sessionId != null) authSessionService.logout(sessionId);
        return logoutResponse();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletRequest request) {
        String sessionId = sessionId(request);
        if (sessionId != null) authQueryService.findActiveSession(sessionId).ifPresent(authSessionService::logoutAll);
        return logoutResponse();
    }

    private ResponseEntity<AuthResponse> buildAuthResponse(LoginResponse response, int status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createSessionCookie(response.sessionId()).toString())
                .body(AuthResponse.from(response));
    }

    private ResponseEntity<Void> logoutResponse() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtils.expireSessionCookie().toString())
                .build();
    }

    private String sessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (sessionConfig.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
