package com.lumie.auth.adapter.in.web;

import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.port.in.OAuth2LoginUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2LoginUseCase oAuth2LoginUseCase;

    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Void> authorize(
            @PathVariable String provider,
            @RequestParam("tenant") String tenantSlug) {
        String authUrl = oAuth2LoginUseCase.getAuthorizationUrl(provider, tenantSlug);
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<LoginResponse> callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        LoginResponse response = oAuth2LoginUseCase.handleCallback(provider, code, state);
        return ResponseEntity.ok(response);
    }
}
