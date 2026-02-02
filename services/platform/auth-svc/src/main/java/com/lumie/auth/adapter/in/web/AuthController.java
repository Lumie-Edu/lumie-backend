package com.lumie.auth.adapter.in.web;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.RefreshTokenRequest;
import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.dto.response.TokenResponse;
import com.lumie.auth.application.port.in.LoginUseCase;
import com.lumie.auth.application.port.in.LogoutUseCase;
import com.lumie.auth.application.port.in.OwnerRegisterUseCase;
import com.lumie.auth.application.port.in.RefreshTokenUseCase;
import com.lumie.auth.application.port.in.RegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = registerUseCase.register(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<LoginResponse> registerOwner(@Valid @RequestBody OwnerRegisterRequest request) {
        LoginResponse response = ownerRegisterUseCase.registerOwner(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = refreshTokenUseCase.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        logoutUseCase.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        logoutUseCase.logoutAll(token);
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
