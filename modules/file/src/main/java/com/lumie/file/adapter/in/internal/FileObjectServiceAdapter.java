package com.lumie.file.adapter.in.internal;

import com.lumie.file.api.FileObjectService;
import com.lumie.file.application.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FileObjectServiceAdapter implements FileObjectService {
    private final StoragePort storagePort;
    @Override public void upload(String key, InputStream content, long size, String type) { storagePort.uploadObject(key, content, size, type); }
    @Override public ObjectStream download(String key) { return new ObjectStream(storagePort.getObject(key), storagePort.objectContentType(key), storagePort.objectSize(key)); }
    @Override public void delete(List<String> keys) { keys.forEach(storagePort::deleteObject); }
    @Override public String presignedDownloadUrl(String key, int seconds) { return storagePort.generatePresignedDownloadUrl(key, seconds); }
    @Override public String presignedUploadUrl(String key, String type, int seconds) { return storagePort.generatePresignedUploadUrl(key, type, seconds); }
    @Override public String presignedInternalDownloadUrl(String key, int seconds) { return storagePort.generateInternalPresignedDownloadUrl(key, seconds); }
    @Override public String presignedInternalUploadUrl(String key, String type, int seconds) { return storagePort.generateInternalPresignedUploadUrl(key, type, seconds); }
}
