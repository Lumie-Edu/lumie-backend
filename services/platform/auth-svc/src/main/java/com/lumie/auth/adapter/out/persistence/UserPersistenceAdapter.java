package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.domain.entity.User;
import com.lumie.auth.domain.vo.Role;
import com.lumie.auth.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA-based implementation of UserLookupPort using Hibernate multi-tenancy.
 * Replaces the JdbcTemplate-based UserSchemaLookupAdapter.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserLookupPort {

    private final UserJpaRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findByEmail(String schemaName, String email) {
        log.debug("Looking up user by email in schema: {}", schemaName);

        setTenantContext(schemaName);
        return userRepository.findByEmail(email)
                .map(this::toUserData);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findById(String schemaName, Long userId) {
        log.debug("Looking up user by ID in schema: {}", schemaName);

        setTenantContext(schemaName);
        return userRepository.findById(userId)
                .map(this::toUserData);
    }

    @Override
    @Transactional
    public UserData findOrCreateOAuth2User(String schemaName, String email, String name, String provider) {
        log.debug("Finding or creating OAuth2 user in schema: {} provider: {}", schemaName, provider);

        setTenantContext(schemaName);

        return userRepository.findByEmail(email)
                .map(this::toUserData)
                .orElseGet(() -> {
                    User newUser = User.createOAuth2User(email, name, provider);
                    User savedUser = userRepository.save(newUser);
                    log.info("Created new OAuth2 user: {} in schema: {}", email, schemaName);
                    return toUserData(savedUser);
                });
    }

    @Override
    @Transactional
    public UserData createUser(String schemaName, String email, String name, String passwordHash, Role role) {
        log.debug("Creating new user in schema: {}", schemaName);

        setTenantContext(schemaName);

        User user = User.create(email, name, passwordHash, role);
        User savedUser = userRepository.save(user);

        log.info("Created new user: {} in schema: {}", email, schemaName);

        return toUserData(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String schemaName, String email) {
        setTenantContext(schemaName);
        return userRepository.existsByEmail(email);
    }

    private void setTenantContext(String schemaName) {
        TenantContext.setTenantId(schemaName);
    }

    private UserData toUserData(User user) {
        return new UserData(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPasswordHash(),
                user.getRole(),
                user.isEnabled()
        );
    }
}
