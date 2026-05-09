package com.lumie.tenant.application.service;

import com.lumie.tenant.application.port.out.TenantLogoStoragePort;
import com.lumie.tenant.application.port.out.TenantLogoStoragePort.LogoStream;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenantLogoService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/svg+xml", "image/webp");
    private static final long MAX_LOGO_BYTES = 5L * 1024 * 1024;

    private final TenantPersistencePort tenantPersistencePort;
    private final TenantLogoStoragePort tenantLogoStoragePort;

    public void uploadLogo(String slug, MultipartFile file) {
        validate(file);
        Tenant tenant = findTenant(slug);
        String objectKey = buildObjectKey(slug);
        try {
            tenantLogoStoragePort.upload(objectKey, file.getInputStream(),
                    file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded logo", e);
        }
        tenant.updateLogoObjectKey(objectKey);
        tenantPersistencePort.save(tenant);
        log.info("Tenant logo uploaded: tenant={}, key={}", slug, objectKey);
    }

    public LogoStream downloadLogo(String slug) {
        Tenant tenant = findTenant(slug);
        String key = tenant.getLogoObjectKey();
        if (key == null || key.isBlank()) {
            return null;
        }
        return tenantLogoStoragePort.download(key);
    }

    public void deleteLogo(String slug) {
        Tenant tenant = findTenant(slug);
        String key = tenant.getLogoObjectKey();
        if (key == null || key.isBlank()) {
            return;
        }
        tenantLogoStoragePort.delete(key);
        tenant.updateLogoObjectKey(null);
        tenantPersistencePort.save(tenant);
        log.info("Tenant logo removed: tenant={}, key={}", slug, key);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is empty");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("Logo exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported logo content type: " + contentType);
        }
    }

    private Tenant findTenant(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", slug));
    }

    private String buildObjectKey(String slug) {
        String tenantId = slug.startsWith("inst-") ? slug.substring(5) : slug;
        return tenantId + "/branding/logo";
    }
}
