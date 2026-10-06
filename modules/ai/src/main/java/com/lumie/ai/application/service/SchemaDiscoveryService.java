package com.lumie.ai.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaDiscoveryService {

    private final JdbcTemplate readOnlyJdbcTemplate;

    private final Map<String, CachedSchema> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour

    @Transactional(readOnly = true)
    public String getSchemaDescription(String tenantSlug) {
        CachedSchema cached = cache.get(tenantSlug);
        if (cached != null && !cached.isExpired()) {
            return cached.description;
        }

        try {
            // Post-RLS the schema is the same across tenants (single
            // public schema), but cache is still tenant-keyed in case
            // future per-tenant feature flags ever diverge the surface.
            List<Map<String, Object>> rows = readOnlyJdbcTemplate.queryForList("""
                SELECT t.table_name, c.column_name, c.data_type, c.udt_name,
                       c.is_nullable, c.column_default,
                       tc.constraint_type
                FROM information_schema.tables t
                JOIN information_schema.columns c
                  ON c.table_schema = t.table_schema AND c.table_name = t.table_name
                LEFT JOIN information_schema.key_column_usage kcu
                  ON kcu.table_schema = c.table_schema
                  AND kcu.table_name = c.table_name
                  AND kcu.column_name = c.column_name
                LEFT JOIN information_schema.table_constraints tc
                  ON tc.constraint_name = kcu.constraint_name
                  AND tc.table_schema = kcu.table_schema
                  AND tc.constraint_type = 'PRIMARY KEY'
                WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
                  AND t.table_name NOT IN ('flyway_schema_history', 'textbooks')
                ORDER BY t.table_name, c.ordinal_position
                """);

            Map<String, List<String>> tables = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String tableName = (String) row.get("table_name");
                String colName = (String) row.get("column_name");
                String dataType = (String) row.get("udt_name");
                String pk = row.get("constraint_type") != null ? " PK" : "";

                String colDesc = colName;
                if ("jsonb".equals(dataType)) {
                    colDesc += " JSONB";
                } else if ("bool".equals(dataType)) {
                    colDesc += " BOOLEAN";
                }
                colDesc += pk;

                tables.computeIfAbsent(tableName, k -> new ArrayList<>()).add(colDesc);
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : tables.entrySet()) {
                sb.append("- ").append(entry.getKey())
                        .append(" (").append(String.join(", ", entry.getValue())).append(")\n");
            }

            String description = sb.toString();
            cache.put(tenantSlug, new CachedSchema(description));
            log.info("Schema discovered for tenant {}: {} tables", tenantSlug, tables.size());
            return description;
        } catch (Exception e) {
            log.error("Failed to discover schema for tenant {}: {}", tenantSlug, e.getMessage());
            return "";
        }
    }

    private record CachedSchema(String description, long createdAt) {
        CachedSchema(String description) {
            this(description, System.currentTimeMillis());
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }
}
