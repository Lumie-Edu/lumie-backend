package com.lumie.homepage.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.homepage.application.dto.request.PublishHomepageRequest;
import com.lumie.homepage.application.dto.request.UpdateHomepageConfigRequest;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.service.HomepageCommandService;
import com.lumie.homepage.application.service.HomepageQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/homepage")
@RequiredArgsConstructor
public class HomepageController {

    private final HomepageQueryService homepageQueryService;
    private final HomepageCommandService homepageCommandService;

    /**
     * GET /v1/homepage — returns the current tenant's homepage config.
     * Returns 404 if no config exists yet.
     * Requires authenticated user (tenant context populated from the
     * X-Tenant-Slug request header by RequestContextFilter).
     */
    @GetMapping
    public ResponseEntity<HomepageConfigResponse> getCurrent() {
        return homepageQueryService.getCurrent()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * PUT /v1/homepage — upserts the homepage config.
     * OWNER role required.
     */
    @PutMapping
    public ResponseEntity<HomepageConfigResponse> update(@Valid @RequestBody UpdateHomepageConfigRequest request) {
        AuthorizationGuard.requireOwner();
        HomepageConfigResponse response = homepageCommandService.update(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /v1/homepage/publish — toggles the publish flag.
     * OWNER role required.
     */
    @PostMapping("/publish")
    public ResponseEntity<HomepageConfigResponse> publish(@Valid @RequestBody PublishHomepageRequest request) {
        AuthorizationGuard.requireOwner();
        HomepageConfigResponse response = homepageCommandService.publish(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/homepage/public/by-custom-id/{customId} — anonymous, no tenant header.
     * Returns config only if published=true; otherwise returns 404.
     */
    @GetMapping("/public/by-custom-id/{customId}")
    public ResponseEntity<HomepageConfigResponse> getPublicByCustomId(@PathVariable String customId) {
        return homepageQueryService.getPublicByCustomId(customId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
