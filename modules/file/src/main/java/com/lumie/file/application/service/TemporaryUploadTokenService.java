package com.lumie.file.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.vo.EntityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class TemporaryUploadTokenService {
    private static final long TTL_SECONDS = 24 * 60 * 60;
    private final byte[] secret;

    public TemporaryUploadTokenService(@Value("${lumie.internal.hmac-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(EntityType type, String objectKey, String filename, String contentType, long fileSize) {
        long expiresAt = Instant.now().getEpochSecond() + TTL_SECONDS;
        return expiresAt + "." + sign(payload(expiresAt, type, objectKey, filename, contentType, fileSize));
    }

    public void verify(EntityType type, TemporaryAttachment attachment) {
        try {
            String[] token = attachment.uploadToken().split("\\.", 2);
            long expiresAt = Long.parseLong(token[0]);
            if (expiresAt < Instant.now().getEpochSecond()
                    || !MessageDigest.isEqual(token[1].getBytes(StandardCharsets.US_ASCII),
                    sign(payload(expiresAt, type, attachment.objectKey(), attachment.filename(), attachment.contentType(), attachment.fileSize())).getBytes(StandardCharsets.US_ASCII))) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
            }
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException) throw exception;
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        }
    }

    private String payload(long expiresAt, EntityType type, String key, String filename, String contentType, long size) {
        return String.join("\n", TenantContextHolder.getRequiredTenant(), String.valueOf(UserContextHolder.getRequiredUserId()),
                type.name(), key, filename, contentType, String.valueOf(size), String.valueOf(expiresAt));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Temporary upload token signing failed", exception);
        }
    }
}
