package com.lumie.auth.adapter.in.web;

import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.port.in.OAuth2LoginUseCase;
import com.lumie.auth.infrastructure.util.SessionMetadataExtractor;
import com.lumie.auth.infrastructure.config.OAuth2Config;
import com.lumie.auth.infrastructure.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/v1/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2LoginUseCase oAuth2LoginUseCase;
    private final CookieUtils cookieUtils;
    private final OAuth2Config oAuth2Config;

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
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest httpRequest) {
        var metadata = SessionMetadataExtractor.extract(httpRequest);
        LoginResponse response = oAuth2LoginUseCase.handleCallback(provider, code, state, metadata);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createAccessTokenCookie(response.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createRefreshTokenCookie(response.refreshToken()).toString())
                .location(URI.create(oAuth2Config.getFrontendCallbackUrl()))
                .build();
    }
}
