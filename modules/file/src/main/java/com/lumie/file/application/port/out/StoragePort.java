package com.lumie.file.application.port.out;

import java.io.InputStream;
import java.time.Instant;

public interface StoragePort {

    String generatePresignedUploadUrl(String objectKey, String contentType, int expiresInSeconds);

    String generatePresignedDownloadUrl(String objectKey, int expiresInSeconds);

    String generateInternalPresignedUploadUrl(String objectKey, String contentType, int expiresInSeconds);

    String generateInternalPresignedDownloadUrl(String objectKey, int expiresInSeconds);

    void uploadObject(String objectKey, InputStream stream, long size, String contentType);

    InputStream getObject(String objectKey);

    void deleteObject(String objectKey);

    boolean objectExists(String objectKey);

    void copyObject(String sourceKey, String destinationKey, String expectedSourceEtag);

    long objectSize(String objectKey);

    String objectEtag(String objectKey);

    String objectContentType(String objectKey);

    Instant objectLastModified(String objectKey);
}
