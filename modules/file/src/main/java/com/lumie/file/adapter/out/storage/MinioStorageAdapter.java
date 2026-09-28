package com.lumie.file.adapter.out.storage;

import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.common.exception.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MinioStorageAdapter implements StoragePort {

    private final MinioClient minioClient;
    private final MinioClient presignMinioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public MinioStorageAdapter(MinioClient minioClient, @Qualifier("presignMinioClient") MinioClient presignMinioClient) {
        this.minioClient = minioClient;
        this.presignMinioClient = presignMinioClient;
    }

    @Override
    public String generatePresignedUploadUrl(String objectKey, String contentType, int expiresInSeconds) {
        try {
            String url = presignMinioClient.getPresignedObjectUrl(
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
            throw new BusinessException(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String objectKey, int expiresInSeconds) {
        return generatePresignedDownloadUrl(presignMinioClient, objectKey, expiresInSeconds);
    }

    @Override
    public String generateInternalPresignedDownloadUrl(String objectKey, int expiresInSeconds) {
        return generatePresignedDownloadUrl(minioClient, objectKey, expiresInSeconds);
    }

    private String generatePresignedDownloadUrl(MinioClient client, String objectKey, int expiresInSeconds) {
        try {
            String url = client.getPresignedObjectUrl(
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
            throw new BusinessException(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    @Override
    public String generateInternalPresignedUploadUrl(String objectKey, String contentType, int expiresInSeconds) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expiresInSeconds, TimeUnit.SECONDS)
                            .build()
            );
            log.debug("Generated internal presigned upload URL for: {}", objectKey);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate internal presigned upload URL for: {}", objectKey, e);
            throw new BusinessException(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    @Override
    public void uploadObject(String objectKey, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("Uploaded object: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to upload object: {}", objectKey, e);
            throw new BusinessException(FileErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get object: {}", objectKey, e);
            throw new BusinessException(FileErrorCode.STORAGE_ERROR);
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
            throw new BusinessException(FileErrorCode.STORAGE_ERROR);
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

    @Override
    public void copyObject(String sourceKey, String destinationKey, String expectedSourceEtag) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder().bucket(bucketName).object(destinationKey)
                    .source(CopySource.builder().bucket(bucketName).object(sourceKey).matchETag(expectedSourceEtag).build()).build());
        } catch (Exception e) {
            log.error("Failed to copy object {} to {}", sourceKey, destinationKey, e);
            throw new BusinessException(FileErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public long objectSize(String objectKey) {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectKey).build()).size();
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public String objectEtag(String objectKey) {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectKey).build()).etag();
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public String objectContentType(String objectKey) {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectKey).build()).contentType();
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }
}
