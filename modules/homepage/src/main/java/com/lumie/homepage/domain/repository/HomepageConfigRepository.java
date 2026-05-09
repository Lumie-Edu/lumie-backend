package com.lumie.homepage.domain.repository;

import com.lumie.homepage.domain.entity.HomepageConfig;

import java.util.Optional;

public interface HomepageConfigRepository {
    HomepageConfig save(HomepageConfig config);
    Optional<HomepageConfig> find();
}
