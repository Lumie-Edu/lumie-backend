package com.lumie.tuition.domain.repository;

import com.lumie.tuition.domain.entity.Guardian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GuardianRepository {

    Guardian save(Guardian guardian);

    Optional<Guardian> findById(Long id);

    Optional<Guardian> findByPhone(String phone);

    Page<Guardian> findAll(Pageable pageable);

    List<Guardian> findByNameContainingIgnoreCase(String name);

    boolean existsByPhone(String phone);

    void delete(Guardian guardian);
}
