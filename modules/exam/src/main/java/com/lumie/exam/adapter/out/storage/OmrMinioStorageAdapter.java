package com.lumie.exam.adapter.out.storage;

import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.file.api.FileObjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Component
public class OmrMinioStorageAdapter implements OmrStoragePort {

    private final FileObjectService fileObjectService;

    public OmrMinioStorageAdapter(FileObjectService fileObjectService) { this.fileObjectService = fileObjectService; }

    @Override
    public String putObject(String objectKey, byte[] data, String contentType) {
        fileObjectService.upload(objectKey, new ByteArrayInputStream(data), data.length, contentType);
        return objectKey;
    }

    @Override
    public byte[] getObject(String objectKey) {
        try (var stream = fileObjectService.download(objectKey).stream()) {
            return stream.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download object: {}", objectKey, e);
            throw new BusinessException(ExamErrorCode.OMR_GRADING_FAILED);
        }
    }

    @Override
    public void deleteObjects(List<String> objectKeys) {
        fileObjectService.delete(objectKeys);
    }

    @Override
    public String getPresignedUrl(String objectKey) {
        return fileObjectService.presignedDownloadUrl(objectKey, 3600);
    }

    @Override
    public String getPresignedPutUrl(String objectKey, int expirySeconds) {
        return fileObjectService.presignedUploadUrl(objectKey, "application/octet-stream", expirySeconds);
    }
}
