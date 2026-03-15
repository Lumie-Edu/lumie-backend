package com.lumie.exam.application.port.out;

import java.util.List;

public interface OmrStoragePort {

    String putObject(String objectKey, byte[] data, String contentType);

    byte[] getObject(String objectKey);

    void deleteObjects(List<String> objectKeys);

    String getPresignedUrl(String objectKey);
}
