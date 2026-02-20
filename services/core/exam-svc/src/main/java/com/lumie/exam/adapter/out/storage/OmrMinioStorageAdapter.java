package com.lumie.exam.adapter.out.storage;

import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OmrMinioStorageAdapter implements OmrStoragePort {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

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
}
