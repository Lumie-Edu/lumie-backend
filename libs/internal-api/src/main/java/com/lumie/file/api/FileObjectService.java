package com.lumie.file.api;

import java.io.InputStream;
import java.util.List;

/** Shared object-storage contract; domain modules never own MinIO clients. */
public interface FileObjectService {
    void upload(String objectKey, InputStream content, long size, String contentType);
    ObjectStream download(String objectKey);
    void delete(List<String> objectKeys);
    String presignedDownloadUrl(String objectKey, int expiresInSeconds);
    String presignedUploadUrl(String objectKey, String contentType, int expiresInSeconds);
    String presignedInternalDownloadUrl(String objectKey, int expiresInSeconds);
    String presignedInternalUploadUrl(String objectKey, String contentType, int expiresInSeconds);
    record ObjectStream(InputStream stream, String contentType, long size) {}
}
