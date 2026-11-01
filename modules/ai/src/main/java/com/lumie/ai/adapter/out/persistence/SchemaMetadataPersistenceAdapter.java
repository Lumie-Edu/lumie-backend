package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.application.port.out.SchemaMetadataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SchemaMetadataPersistenceAdapter implements SchemaMetadataPort {

    private static final String DISCOVER_COLUMNS_SQL = """
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
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SchemaColumn> discoverColumns() {
        return jdbcTemplate.query(DISCOVER_COLUMNS_SQL, (resultSet, rowNum) -> new SchemaColumn(
                resultSet.getString("table_name"),
                resultSet.getString("column_name"),
                resultSet.getString("udt_name"),
                resultSet.getString("constraint_type")));
    }
}
