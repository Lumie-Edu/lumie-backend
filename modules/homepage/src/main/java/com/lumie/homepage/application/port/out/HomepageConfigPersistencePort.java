package com.lumie.homepage.application.port.out;

import com.lumie.homepage.domain.entity.HomepageConfig;

import java.util.Optional;

public interface HomepageConfigPersistencePort {
    HomepageConfig save(HomepageConfig config);
    Optional<HomepageConfig> find();
}
