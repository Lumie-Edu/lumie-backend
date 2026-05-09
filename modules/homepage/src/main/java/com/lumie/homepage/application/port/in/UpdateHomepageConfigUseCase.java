package com.lumie.homepage.application.port.in;

import com.lumie.homepage.application.dto.request.UpdateHomepageConfigRequest;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;

public interface UpdateHomepageConfigUseCase {
    HomepageConfigResponse update(UpdateHomepageConfigRequest request);
}
