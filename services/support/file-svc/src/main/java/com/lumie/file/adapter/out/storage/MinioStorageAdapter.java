package com.lumie.file.adapter.out.storage;

import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.exception.FileException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageAdapter implements StoragePort {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public String generatePresignedUploadUrl(String objectKey, String contentType, int expiresInSeconds) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expiresInSeconds, TimeUnit.SECONDS)
                            .build()
            );
            log.debug("Generated presigned upload URL for: {}", objectKey);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for: {}", objectKey, e);
            throw new FileException(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String objectKey, int expiresInSeconds) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expiresInSeconds, TimeUnit.SECONDS)
                            .build()
            );
            log.debug("Generated presigned download URL for: {}", objectKey);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL for: {}", objectKey, e);
            throw new FileException(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            log.debug("Deleted object: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete object: {}", objectKey, e);
            throw new FileException(FileErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.debug("Object not found: {}", objectKey);
            return false;
        }
    }
}
