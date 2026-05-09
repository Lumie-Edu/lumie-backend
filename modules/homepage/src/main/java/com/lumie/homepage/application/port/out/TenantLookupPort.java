package com.lumie.homepage.application.port.out;

import java.util.Optional;

public interface TenantLookupPort {

    Optional<TenantInfo> findByCustomId(String customId);

    record TenantInfo(String slug, String schemaName) {}
}
