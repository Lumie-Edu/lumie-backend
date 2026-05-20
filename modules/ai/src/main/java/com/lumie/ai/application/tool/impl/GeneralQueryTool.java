package com.lumie.ai.application.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.tool.*;
import com.lumie.ai.application.service.SqlValidator;
import com.lumie.ai.adapter.out.config.ReadOnlyDataSourceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeneralQueryTool implements Tool {

    private final JdbcTemplate readOnlyJdbcTemplate;
    private final SqlValidator sqlValidator;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "general_query";
    }

    @Override
    public String getDescription() {
        return "Execute a read-only SQL query against the tenant database to retrieve information about students, academies, exams, announcements, attendance, etc.";
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
                                        "description", "SELECT SQL query to execute. Must be read-only. Available tables: students, admins, academies, announcements, exams, exam_results, counseling_schedules, counseling_reservations, qna_boards, qna_comments, textbooks, reviews, toggles. Students and admins reference public.users for login info."
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
    @Transactional(readOnly = true)
    public ToolResult execute(String tenantSlug, Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");

        try {
            sqlValidator.validate(sql);

            Long tenantId = com.lumie.common.tenant.TenantContextHolder.getRequiredTenantId();
            ReadOnlyDataSourceConfig.setTenantSchema(readOnlyJdbcTemplate, tenantId);

            List<Map<String, Object>> results = readOnlyJdbcTemplate.queryForList(sql);

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
}
