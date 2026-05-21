package com.lumie.tenant.adapter.out.storage;

import com.lumie.common.exception.BusinessException;
import com.lumie.tenant.application.port.out.TenantLogoStoragePort;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantLogoStorageAdapter implements TenantLogoStoragePort {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucket;

    @Override
    public void upload(String objectKey, InputStream content, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload tenant logo: {}", objectKey, e);
            throw new BusinessException(TenantErrorCode.LOGO_UPLOAD_FAILED, e);
        }
    }

    @Override
    public LogoStream download(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            return new LogoStream(stream, stat.contentType(), stat.size());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return null;
            }
            log.error("Failed to download tenant logo: {}", objectKey, e);
            throw new BusinessException(TenantErrorCode.LOGO_DOWNLOAD_FAILED, e);
        } catch (Exception e) {
            log.error("Failed to download tenant logo: {}", objectKey, e);
            throw new BusinessException(TenantErrorCode.LOGO_DOWNLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete tenant logo (continuing): {}", objectKey, e);
        }
    }
}
