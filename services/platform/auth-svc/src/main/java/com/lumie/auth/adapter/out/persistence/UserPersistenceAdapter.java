package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.domain.entity.User;
import com.lumie.auth.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA-based implementation of UserLookupPort.
 * Queries the public.users table directly (no multi-tenancy schema switching needed).
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserLookupPort {

    private final UserJpaRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findByUserLoginId(String userLoginId) {
        log.debug("Looking up user by login ID: {}", userLoginId);
        return userRepository.findByUserLoginId(userLoginId)
                .map(this::toUserData);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findById(Long userId) {
        log.debug("Looking up user by ID: {}", userId);
        return userRepository.findById(userId)
                .map(this::toUserData);
    }

    @Override
    @Transactional
    public UserData findOrCreateOAuth2User(String userLoginId, String name, Long tenantId, String provider) {
        log.debug("Finding or creating OAuth2 user: {} provider: {}", userLoginId, provider);

        return userRepository.findByUserLoginId(userLoginId)
                .map(this::toUserData)
                .orElseGet(() -> {
                    User newUser = User.createOAuth2User(userLoginId, name, tenantId, provider);
                    User savedUser = userRepository.save(newUser);
                    log.info("Created new OAuth2 user: {} for tenant: {}", userLoginId, tenantId);
                    return toUserData(savedUser);
                });
    }

    @Override
    @Transactional
    public UserData createUser(String userLoginId, String name, String phone, String passwordHash, Role role, Long tenantId) {
        log.debug("Creating new user: {} for tenant: {}", userLoginId, tenantId);

        User user = User.create(userLoginId, name, phone, passwordHash, role, tenantId);
        User savedUser = userRepository.save(user);

        log.info("Created new user: {} for tenant: {}", userLoginId, tenantId);

        return toUserData(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserLoginId(String userLoginId) {
        return userRepository.existsByUserLoginId(userLoginId);
    }

    @Override
    @Transactional
    public boolean deleteUser(Long userId) {
        log.debug("Deleting user by ID: {}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    userRepository.delete(user);
                    log.info("Deleted user: {}", userId);
                    return true;
                })
                .orElse(false);
    }

    private UserData toUserData(User user) {
        return new UserData(
                user.getId(),
                user.getUserLoginId(),
                user.getName(),
                user.getPhone(),
                user.getPasswordHash(),
                user.getRole(),
                user.getTenantId(),
                user.isEnabled()
        );
    }
}
