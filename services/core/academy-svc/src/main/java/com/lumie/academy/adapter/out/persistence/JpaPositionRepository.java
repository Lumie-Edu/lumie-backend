package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaPositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByName(String name);

    List<Position> findByIsActiveTrue();

    boolean existsByName(String name);
}
