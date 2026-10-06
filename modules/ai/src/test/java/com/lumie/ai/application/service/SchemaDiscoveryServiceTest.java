package com.lumie.ai.application.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaDiscoveryServiceTest {

    @Test
    void excludesRetiredTextbookTableFromChatbotSchema() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

        new SchemaDiscoveryService(jdbcTemplate).getSchemaDescription("joossameng");

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(query.capture());
        assertTrue(query.getValue().contains("t.table_name NOT IN ('flyway_schema_history', 'textbooks')"));
    }
}
