package com.lumie.ai.application.service;

import com.lumie.ai.adapter.out.persistence.SchemaMetadataPersistenceAdapter;
import com.lumie.ai.application.port.out.SchemaMetadataPort.SchemaColumn;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchemaDiscoveryServiceTest {

    @Test
    void excludesRetiredTextbookTableFromChatbotSchema() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        new SchemaMetadataPersistenceAdapter(jdbcTemplate).discoverColumns();

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).<SchemaColumn>query(
                query.capture(), ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<SchemaColumn>>any());
        assertTrue(query.getValue().contains("t.table_name NOT IN ('flyway_schema_history', 'textbooks')"));
    }
}
