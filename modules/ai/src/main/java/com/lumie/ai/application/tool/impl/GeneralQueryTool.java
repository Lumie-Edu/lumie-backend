package com.lumie.ai.application.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.tool.*;
import com.lumie.ai.application.service.SqlValidator;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeneralQueryTool implements Tool {

    /** Hard cap on rows returned when the model omits its own LIMIT. */
    private static final int MAX_ROWS = 500;
    /** Per-statement timeout so a runaway/cartesian query cannot pin a connection. */
    private static final String STATEMENT_TIMEOUT = "10s";
    private static final Pattern LIMIT_PATTERN =
            Pattern.compile("\\bLIMIT\\b", Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate readOnlyJdbcTemplate;
    private final SqlValidator sqlValidator;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "general_query";
    }

    @Override
    public String getDescription() {
        return "Execute a read-only SQL query against the tenant database to retrieve information about students, academies, exams, announcements, etc.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sql", Map.of(
                                        "type", "string",
                                        "description", "SELECT SQL query to execute. Must be read-only. Available tables: students, staff, academies, announcements, exams, exam_results, qna_boards, qna_comments, file_metadata, reviews, toggles. Teaching materials are file_metadata rows with entity_type = 'TEXTBOOK' and upload_completed = true; use original_filename for the file name. Students and staff reference public.users for login info."
                                )
                        ),
                        "required", List.of("sql")
                )
        );
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.READ;
    }

    @Override
    public ToolResult execute(String tenantSlug, Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");

        try {
            sqlValidator.validate(sql);
            String boundedSql = enforceRowLimit(sql);
            Long tenantId = TenantContextHolder.getRequiredTenantId();

            List<Map<String, Object>> results = runTenantScopedQuery(tenantId, boundedSql);

            if (results.isEmpty()) {
                return ToolResult.success("No results found.");
            }

            String json = objectMapper.writeValueAsString(results);
            if (json.length() > 10000) {
                json = json.substring(0, 10000) + "... (truncated, " + results.size() + " rows total)";
            }
            return ToolResult.success(json);
        } catch (Exception e) {
            log.error("Query execution failed for tenant {}: {}", tenantSlug, e.getMessage());
            return ToolResult.failure("Query failed: " + e.getMessage());
        }
    }

    /**
     * Runs the RLS GUC, the statement timeout, and the query on ONE read-only
     * connection inside ONE explicit transaction. This is required because
     * {@code readOnlyJdbcTemplate} wraps a standalone HikariCP pool that is NOT
     * enrolled in Spring's {@code @Transactional} (a different DataSource), so a
     * {@code SET LOCAL} issued on its own would evaporate after that single
     * auto-commit statement and the query would run with {@code app.tenant_id}
     * unset (RLS → zero rows). Scoping all three to one manual transaction makes
     * {@code SET LOCAL} persist for the query; {@code is_local=true} also means
     * the GUC is dropped at commit, so the pooled connection is clean for reuse.
     */
    private List<Map<String, Object>> runTenantScopedQuery(Long tenantId, String sql) {
        return readOnlyJdbcTemplate.execute((Connection con) -> {
            boolean prevAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try (Statement stmt = con.createStatement()) {
                stmt.execute("SET LOCAL statement_timeout = '" + STATEMENT_TIMEOUT + "'");
                // tenantId is a Long → safe to inline; binds RLS for this transaction.
                stmt.execute("SELECT set_config('app.tenant_id', '" + tenantId + "', true)");
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    List<Map<String, Object>> rows = new RowMapperResultSetExtractor<>(
                            new ColumnMapRowMapper()).extractData(rs);
                    con.commit();
                    return rows;
                }
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(prevAutoCommit);
            }
        });
    }

    /**
     * Appends a row cap when the model did not write its own LIMIT, so an
     * unbounded {@code SELECT * FROM students} cannot stream an entire tenant.
     * Appending (rather than wrapping in a subquery) avoids the duplicate-column
     * error that {@code SELECT * FROM (<sql>) _q} would raise on joined queries.
     * The LIMIT is placed on its own line so a trailing {@code -- comment} on the
     * model's last line cannot comment it out.
     */
    private String enforceRowLimit(String sql) {
        String trimmed = sql.strip();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
        }
        if (LIMIT_PATTERN.matcher(trimmed).find()) {
            return trimmed;
        }
        return trimmed + "\nLIMIT " + MAX_ROWS;
    }
}
