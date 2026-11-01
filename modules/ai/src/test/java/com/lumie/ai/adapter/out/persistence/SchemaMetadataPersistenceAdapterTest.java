package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.application.port.out.SchemaMetadataPort.SchemaColumn;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
class SchemaMetadataPersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Test
    void discoversPublicColumnsAndPrimaryKeysButSkipsInternalTables() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE schema_probe (id BIGINT PRIMARY KEY, display_name TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE textbooks (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE flyway_schema_history (id BIGINT PRIMARY KEY)");

        var columns = new SchemaMetadataPersistenceAdapter(jdbc).discoverColumns();

        assertThat(columns).extracting(SchemaColumn::tableName)
                .contains("schema_probe")
                .doesNotContain("textbooks", "flyway_schema_history");
        assertThat(columns).filteredOn(column -> column.tableName().equals("schema_probe"))
                .extracting(SchemaColumn::columnName)
                .containsExactly("id", "display_name");
        assertThat(columns).filteredOn(column -> column.columnName().equals("id"))
                .extracting(SchemaColumn::udtName, SchemaColumn::constraintType)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("int8", "PRIMARY KEY"));
    }
}
