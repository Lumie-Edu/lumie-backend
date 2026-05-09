package com.lumie.homepage.application.port.in;

import com.lumie.homepage.application.dto.response.HomepageConfigResponse;

import java.util.Optional;

public interface GetHomepageConfigUseCase {
    Optional<HomepageConfigResponse> getCurrent();
    Optional<HomepageConfigResponse> getPublicByCustomId(String customId);
}
