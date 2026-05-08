package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.domain.entity.User;
import com.lumie.auth.domain.vo.Role;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Tenant-scoped JPA implementation of {@link UserLookupPort}. All operations
 * require {@code TenantContextHolder} to be set; Hibernate multi-tenancy
 * routes queries to the correct {@code tenant_*.users} table.
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
    public UserData findOrCreateOAuth2User(String userLoginId, String name, String provider) {
        log.debug("Finding or creating OAuth2 user: {} provider: {}", userLoginId, provider);

        return userRepository.findByUserLoginId(userLoginId)
                .map(this::toUserData)
                .orElseGet(() -> {
                    User newUser = User.createOAuth2User(userLoginId, name, Role.STUDENT);
                    User savedUser = userRepository.save(newUser);
                    log.info("Created new OAuth2 user: {} via {} in tenant: {}",
                            userLoginId, provider, TenantContextHolder.getTenant());
                    return toUserData(savedUser);
                });
    }

    @Override
    @Transactional
    public UserData createUser(String userLoginId, String name, String phone, String passwordHash, Role role) {
        log.debug("Creating new user: {} in tenant: {}", userLoginId, TenantContextHolder.getTenant());

        User user = User.create(userLoginId, name, phone, passwordHash, role);
        User savedUser = userRepository.save(user);

        log.info("Created new user: {} (id={}) in tenant: {}",
                userLoginId, savedUser.getId(), TenantContextHolder.getTenant());

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

    @Override
    @Transactional
    public boolean resetPassword(Long userId, String newPasswordHash) {
        log.debug("Resetting password for user ID: {}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    user.resetPassword(newPasswordHash);
                    userRepository.save(user);
                    log.info("Password reset for user: {}", userId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public ChangeLoginIdResult changeLoginId(Long userId, String newLoginId) {
        log.debug("Changing login ID for user ID: {}", userId);
        if (userRepository.existsByUserLoginId(newLoginId)) {
            return new ChangeLoginIdResult(false, "Login ID already in use: " + newLoginId, null);
        }
        return userRepository.findById(userId)
                .map(user -> {
                    user.changeLoginId(newLoginId);
                    userRepository.save(user);
                    log.info("Login ID changed for user: {}", userId);
                    return new ChangeLoginIdResult(true, "Login ID changed successfully", newLoginId);
                })
                .orElse(new ChangeLoginIdResult(false, "User not found: " + userId, null));
    }

    @Override
    @Transactional
    public boolean updateProfile(Long userId, String name, String phone, String email) {
        log.debug("Updating profile for user ID: {}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    user.updateProfile(name, phone, email);
                    userRepository.save(user);
                    log.info("Profile updated for user: {}", userId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean updateAvatarSeed(Long userId, String avatarSeed) {
        log.debug("Updating avatar seed for user ID: {}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    user.setAvatarSeed(avatarSeed);
                    userRepository.save(user);
                    log.info("Avatar seed updated for user: {}", userId);
                    return true;
                })
                .orElse(false);
    }

    private UserData toUserData(User user) {
        Long tenantId = TenantContextHolder.getTenantId();
        return new UserData(
                user.getId(),
                user.getUserLoginId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                tenantId,
                user.isEnabled(),
                user.getAvatarSeed()
        );
    }
}
