package com.lumie.ai.adapter.out.ai;

import com.lumie.ai.application.tool.ToolResult;
import com.lumie.ai.application.tool.impl.GeneralQueryTool;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LumieReadTools {

    private final GeneralQueryTool generalQueryTool;

    @Tool(description = "Execute a read-only SQL query against the tenant database to retrieve information " +
            "about students, academies, exams, announcements, attendance, etc. Only SELECT queries are allowed.")
    public String generalQuery(@ToolParam(description = "SELECT SQL query to execute") String sql) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        ToolResult result = generalQueryTool.execute(tenantSlug, Map.of("sql", sql));
        return result.success() ? result.data() : "Error: " + result.error();
    }
}
