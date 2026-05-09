package com.lumie.homepage.adapter.out.persistence;

import com.lumie.homepage.domain.entity.HomepageConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomepageConfigJpaRepository extends JpaRepository<HomepageConfig, Long> {

    Optional<HomepageConfig> findFirstByOrderByIdAsc();
}
