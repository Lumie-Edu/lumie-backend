package com.lumie.auth.adapter.out.external;

import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSchemaLookupAdapter implements UserLookupPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<UserData> findByEmail(String schemaName, String email) {
        log.debug("Looking up user by email in schema: {}", schemaName);

        String sql = String.format(
                "SELECT id, email, name, password_hash, role, enabled FROM %s.users WHERE email = ?",
                sanitizeSchemaName(schemaName)
        );

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new UserData(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("name"),
                            rs.getString("password_hash"),
                            Role.fromString(rs.getString("role")),
                            rs.getBoolean("enabled")
                    ),
                    email
            ));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserData> findById(String schemaName, Long userId) {
        log.debug("Looking up user by ID in schema: {}", schemaName);

        String sql = String.format(
                "SELECT id, email, name, password_hash, role, enabled FROM %s.users WHERE id = ?",
                sanitizeSchemaName(schemaName)
        );

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new UserData(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("name"),
                            rs.getString("password_hash"),
                            Role.fromString(rs.getString("role")),
                            rs.getBoolean("enabled")
                    ),
                    userId
            ));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public UserData findOrCreateOAuth2User(String schemaName, String email, String name, String provider) {
        log.debug("Finding or creating OAuth2 user in schema: {} provider: {}", schemaName, provider);

        // Try to find existing user
        Optional<UserData> existingUser = findByEmail(schemaName, email);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Create new user
        String sanitizedSchema = sanitizeSchemaName(schemaName);
        String insertSql = String.format(
                "INSERT INTO %s.users (email, name, password_hash, role, enabled, oauth_provider) " +
                        "VALUES (?, ?, '', 'STUDENT', true, ?) RETURNING id",
                sanitizedSchema
        );

        Long newUserId = jdbcTemplate.queryForObject(insertSql, Long.class, email, name, provider);

        log.info("Created new OAuth2 user: {} in schema: {}", email, schemaName);

        return new UserData(
                newUserId,
                email,
                name,
                "",
                Role.STUDENT,
                true
        );
    }

    private String sanitizeSchemaName(String schemaName) {
        if (!schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        return schemaName;
    }
}
