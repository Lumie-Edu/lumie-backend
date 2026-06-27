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
 * JPA repository for RLS-scoped {@link User} rows.
 *
 * <p>Queries run against the regular {@code users} table with
 * {@code app.tenant_id} bound by {@code TenantContextHolder}; root-entry owner
 * lookup uses {@code public.owner_directory} through {@code UserDirectoryPort}.
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
