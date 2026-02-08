package com.lumie.academy.domain.repository;

import com.lumie.academy.domain.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PositionRepository {

    Position save(Position position);

    Optional<Position> findById(Long id);

    Optional<Position> findByName(String name);

    Page<Position> findAll(Pageable pageable);

    List<Position> findByIsActiveTrue();

    boolean existsByName(String name);

    void delete(Position position);
}
