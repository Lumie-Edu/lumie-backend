package com.lumie.homepage.adapter.out.persistence;

import com.lumie.homepage.application.port.out.HomepageConfigPersistencePort;
import com.lumie.homepage.domain.entity.HomepageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HomepageConfigPersistenceAdapter implements HomepageConfigPersistencePort {

    private final HomepageConfigJpaRepository homepageConfigJpaRepository;

    @Override
    public HomepageConfig save(HomepageConfig config) {
        return homepageConfigJpaRepository.save(config);
    }

    @Override
    public Optional<HomepageConfig> find() {
        return homepageConfigJpaRepository.findFirstByOrderByIdAsc();
    }
}
