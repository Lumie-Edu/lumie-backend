package com.lumie.ai.domain.service;

import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SqlValidator {

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE",
            "TRUNCATE", "GRANT", "REVOKE", "EXECUTE", "EXEC",
            "INTO", "COPY", "VACUUM", "REINDEX", "CLUSTER"
    );

    private static final Pattern SEMICOLON_PATTERN = Pattern.compile(";");
    private static final Pattern PG_CATALOG_PATTERN = Pattern.compile(
            "\\bpg_\\w+", Pattern.CASE_INSENSITIVE);
    private static final Pattern INFORMATION_SCHEMA_PATTERN = Pattern.compile(
            "\\binformation_schema\\b", Pattern.CASE_INSENSITIVE);

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new AiException(AiErrorCode.INVALID_SQL, "SQL query cannot be empty");
        }

        String trimmed = sql.strip();

        if (SEMICOLON_PATTERN.matcher(trimmed).find()) {
            throw new AiException(AiErrorCode.INVALID_SQL,
                    "Multiple statements not allowed (semicolons detected)");
        }

        String upper = trimmed.toUpperCase();

        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw new AiException(AiErrorCode.INVALID_SQL,
                    "Only SELECT (or WITH ... SELECT) queries are allowed");
        }

        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(trimmed).find()) {
                throw new AiException(AiErrorCode.INVALID_SQL,
                        "Forbidden keyword detected: " + keyword);
            }
        }

        if (PG_CATALOG_PATTERN.matcher(trimmed).find()) {
            throw new AiException(AiErrorCode.INVALID_SQL,
                    "Access to pg_ catalog tables is not allowed");
        }

        if (INFORMATION_SCHEMA_PATTERN.matcher(trimmed).find()) {
            throw new AiException(AiErrorCode.INVALID_SQL,
                    "Access to information_schema is not allowed");
        }
    }
}
