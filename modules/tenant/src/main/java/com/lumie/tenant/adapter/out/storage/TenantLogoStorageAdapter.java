package com.lumie.tenant.adapter.out.storage;

import com.lumie.tenant.application.port.out.TenantLogoStoragePort;
import com.lumie.file.api.FileObjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantLogoStorageAdapter implements TenantLogoStoragePort {

    private final FileObjectService fileObjectService;

    @Override
    public void upload(String objectKey, InputStream content, long size, String contentType) {
        fileObjectService.upload(objectKey, content, size, contentType);
    }

    @Override
    public LogoStream download(String objectKey) {
        FileObjectService.ObjectStream object = fileObjectService.download(objectKey);
        return new LogoStream(object.stream(), object.contentType(), object.size());
    }

    @Override
    public void delete(String objectKey) {
        fileObjectService.delete(java.util.List.of(objectKey));
    }
}
