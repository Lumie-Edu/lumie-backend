package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.application.port.out.UserDirectoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementation of {@link UserDirectoryPort} using raw JDBC against
 * {@code public.owner_directory} — the RLS-exempt platform identity-routing
 * table (login id &rarr; tenant). It is queried at login time BEFORE any
 * tenant context exists, so it must not be the RLS-guarded {@code public.users}
 * table (which returns zero rows without {@code app.tenant_id} set).
 *
 * <p>Columns: {@code id}, {@code user_login_id}, {@code tenant_id}, {@code email}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDirectoryPersistenceAdapter implements UserDirectoryPort {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<DirectoryEntry> ROW_MAPPER = (rs, rowNum) -> new DirectoryEntry(
            rs.getLong("id"),
            rs.getString("user_login_id"),
            rs.getLong("tenant_id"),
            rs.getString("email")
    );

    @Override
    public Optional<DirectoryEntry> findByUserLoginId(String userLoginId) {
        log.debug("Directory lookup by user_login_id: {}", userLoginId);
        try {
            DirectoryEntry entry = jdbcTemplate.queryForObject(
                    "SELECT id, user_login_id, tenant_id, email " +
                            "FROM public.owner_directory " +
                            "WHERE user_login_id = ?",
                    ROW_MAPPER,
                    userLoginId
            );
            return Optional.ofNullable(entry);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<DirectoryEntry> findById(Long userId) {
        log.debug("Directory lookup by id: {}", userId);
        try {
            DirectoryEntry entry = jdbcTemplate.queryForObject(
                    "SELECT id, user_login_id, tenant_id, email " +
                            "FROM public.owner_directory " +
                            "WHERE id = ?",
                    ROW_MAPPER,
                    userId
            );
            return Optional.ofNullable(entry);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void createDirectoryEntry(Long userId, String userLoginId, Long tenantId, String email) {
        log.info("Creating directory entry: id={}, userLoginId={}, tenantId={}", userId, userLoginId, tenantId);
        jdbcTemplate.update(
                "INSERT INTO public.owner_directory (id, user_login_id, tenant_id, email) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT (id) DO NOTHING",
                userId, userLoginId, tenantId, email
        );
    }

    @Override
    public void updateEmail(Long userId, String email) {
        jdbcTemplate.update(
                "UPDATE public.owner_directory SET email = ? WHERE id = ?",
                email, userId
        );
    }

    @Override
    public void updateLoginId(Long userId, String newLoginId) {
        jdbcTemplate.update(
                "UPDATE public.owner_directory SET user_login_id = ? WHERE id = ?",
                newLoginId, userId
        );
    }

    @Override
    public void deleteDirectoryEntry(Long userId) {
        log.info("Deleting directory entry: id={}", userId);
        jdbcTemplate.update(
                "DELETE FROM public.owner_directory WHERE id = ?",
                userId
        );
    }
}
