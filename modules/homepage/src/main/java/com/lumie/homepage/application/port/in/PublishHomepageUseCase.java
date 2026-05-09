package com.lumie.homepage.application.port.in;

import com.lumie.homepage.application.dto.request.PublishHomepageRequest;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;

public interface PublishHomepageUseCase {
    HomepageConfigResponse publish(PublishHomepageRequest request);
}
