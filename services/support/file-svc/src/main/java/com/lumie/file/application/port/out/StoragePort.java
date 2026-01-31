package com.lumie.file.application.port.out;

public interface StoragePort {

    String generatePresignedUploadUrl(String objectKey, String contentType, int expiresInSeconds);

    String generatePresignedDownloadUrl(String objectKey, int expiresInSeconds);

    void deleteObject(String objectKey);

    boolean objectExists(String objectKey);
}
