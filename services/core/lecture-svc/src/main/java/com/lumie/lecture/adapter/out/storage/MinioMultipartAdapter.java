package com.lumie.lecture.adapter.out.storage;

import com.lumie.common.exception.BusinessException;
import com.lumie.lecture.application.port.out.LectureStoragePort;
import com.lumie.lecture.domain.exception.LectureErrorCode;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MinioMultipartAdapter implements LectureStoragePort {

    private final MinioClient minioClient;
    private final MinioClient presignMinioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final int UPLOAD_URL_EXPIRY_SECONDS = 7200; // 2 hours for large files
    private static final int DOWNLOAD_URL_EXPIRY_SECONDS = 3600;

    public MinioMultipartAdapter(MinioClient minioClient,
                                  @Qualifier("presignMinioClient") MinioClient presignMinioClient) {
        this.minioClient = minioClient;
        this.presignMinioClient = presignMinioClient;
    }

    @Override
    public String generatePresignedUploadUrl(String objectKey, String contentType) {
        try {
            String url = presignMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(UPLOAD_URL_EXPIRY_SECONDS, TimeUnit.SECONDS)
                            .build()
            );
            log.debug("Generated presigned upload URL for: {}", objectKey);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for: {}", objectKey, e);
            throw new BusinessException(LectureErrorCode.PRESIGNED_URL_FAILED, e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String objectKey, int expiresInSeconds) {
        try {
            String url = presignMinioClient.getPresignedObjectUrl(
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
            throw new BusinessException(LectureErrorCode.PRESIGNED_URL_FAILED, e);
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
            return false;
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
            throw new BusinessException(LectureErrorCode.STORAGE_ERROR, e);
        }
    }
}
