package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for User entity.
 * Queries the public.users table directly (no multi-tenancy schema switching).
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserLoginId(String userLoginId);

    boolean existsByUserLoginId(String userLoginId);
}
