package com.lumie.tenant.application.port.out;

import java.io.InputStream;

public interface TenantLogoStoragePort {

    void upload(String objectKey, InputStream content, long size, String contentType);

    LogoStream download(String objectKey);

    void delete(String objectKey);

    record LogoStream(InputStream stream, String contentType, long size) {}
}
