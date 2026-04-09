package com.lumie.lecture.application.port.out;

public interface LectureStoragePort {

    String generatePresignedUploadUrl(String objectKey, String contentType);

    String generatePresignedDownloadUrl(String objectKey, int expiresInSeconds);

    boolean objectExists(String objectKey);

    void deleteObject(String objectKey);
}
