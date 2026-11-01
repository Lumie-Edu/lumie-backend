package com.lumie.ai.application.service;

import com.lumie.ai.application.port.out.SchemaMetadataPort;
import com.lumie.ai.application.port.out.SchemaMetadataPort.SchemaColumn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaDiscoveryService {

    private final SchemaMetadataPort schemaMetadataPort;

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
            List<SchemaColumn> rows = schemaMetadataPort.discoverColumns();
            Map<String, List<String>> tables = new LinkedHashMap<>();
            for (SchemaColumn row : rows) {
                String tableName = row.tableName();
                String colName = row.columnName();
                String dataType = row.udtName();
                String pk = row.constraintType() != null ? " PK" : "";

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
