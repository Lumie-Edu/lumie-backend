package com.lumie.tenant.adapter.in.web;

import com.lumie.common.tenant.RequestContextFilter;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.service.HostTenantResolver;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Entry point used by the wildcard tenant host route for its exact root path. */
@RestController
@Hidden
public class TenantRootController {

    private static final String REPLACED_PATH_HEADER = "X-Replaced-Path";

    @GetMapping("/tenant-root")
    public ResponseEntity<Void> redirectToTenantLogin(HttpServletRequest request) {
        Object value = request.getAttribute(RequestContextFilter.HOST_TENANT_RESOLUTION_ATTRIBUTE);
        if (!(value instanceof HostTenantResolver.ResolvedTenant tenant)) {
            throw new ResourceNotFoundException("Tenant", "request host");
        }

        if (tenant.customDomain() != null) {
            String query = request.getQueryString();
            String path = safeOriginalPath(request.getHeader(REPLACED_PATH_HEADER));
            String location = "https://" + tenant.customDomain() + path + (query == null ? "" : "?" + query);
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .location(URI.create(location))
                    .build();
        }

        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create("/login"))
                .build();
    }

    private String safeOriginalPath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String candidate = value.trim();
        try {
            URI uri = URI.create(candidate);
            if (uri.isAbsolute()
                    || uri.getRawAuthority() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getRawPath() == null
                    || !uri.getRawPath().startsWith("/")) {
                return "/";
            }
            return uri.getRawPath();
        } catch (IllegalArgumentException exception) {
            return "/";
        }
    }
}
