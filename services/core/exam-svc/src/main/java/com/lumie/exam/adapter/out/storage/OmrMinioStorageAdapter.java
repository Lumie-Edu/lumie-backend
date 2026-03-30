package com.lumie.exam.adapter.out.storage;

import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OmrMinioStorageAdapter implements OmrStoragePort {

    private final MinioClient minioClient;
    private final MinioClient presignMinioClient;
    private final String bucketName;

    public OmrMinioStorageAdapter(
            MinioClient minioClient,
            @Qualifier("presignMinioClient") MinioClient presignMinioClient,
            @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.presignMinioClient = presignMinioClient;
        this.bucketName = bucketName;
    }

    @Override
    public String putObject(String objectKey, byte[] data, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("Uploaded object: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload object: {}", objectKey, e);
            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED);
        }
    }

    @Override
    public byte[] getObject(String objectKey) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()
        )) {
            return stream.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download object: {}", objectKey, e);
            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED);
        }
    }

    @Override
    public void deleteObjects(List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .build()
                );
                log.debug("Deleted object: {}", objectKey);
            } catch (Exception e) {
                log.warn("Failed to delete object: {}", objectKey, e);
            }
        }
    }

    @Override
    public String getPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", objectKey, e);
            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED);
        }
    }

    @Override
    public String getPresignedPutUrl(String objectKey, int expirySeconds) {
        try {
            return presignMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT URL for: {}", objectKey, e);
            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED);
        }
    }
}
