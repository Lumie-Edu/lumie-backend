package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for User entity.
 * Queries the public.users table directly (no multi-tenancy schema switching).
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserLoginId(String userLoginId);

    boolean existsByUserLoginId(String userLoginId);

    @Query("SELECT u.id AS id, u.avatarSeed AS avatarSeed FROM User u WHERE u.id IN :ids AND u.avatarSeed IS NOT NULL")
    List<UserAvatarRow> findAvatarSeedsByIds(@Param("ids") Collection<Long> ids);

    interface UserAvatarRow {
        Long getId();
        String getAvatarSeed();
    }
}
