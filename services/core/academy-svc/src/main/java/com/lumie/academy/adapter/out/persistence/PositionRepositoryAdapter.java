package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Position;
import com.lumie.academy.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PositionRepositoryAdapter implements PositionRepository {

    private final JpaPositionRepository jpaPositionRepository;

    @Override
    public Position save(Position position) {
        return jpaPositionRepository.save(position);
    }

    @Override
    public Optional<Position> findById(Long id) {
        return jpaPositionRepository.findById(id);
    }

    @Override
    public Optional<Position> findByName(String name) {
        return jpaPositionRepository.findByName(name);
    }

    @Override
    public Page<Position> findAll(Pageable pageable) {
        return jpaPositionRepository.findAll(pageable);
    }

    @Override
    public List<Position> findByIsActiveTrue() {
        return jpaPositionRepository.findByIsActiveTrue();
    }

    @Override
    public boolean existsByName(String name) {
        return jpaPositionRepository.existsByName(name);
    }

    @Override
    public void delete(Position position) {
        jpaPositionRepository.delete(position);
    }
}
