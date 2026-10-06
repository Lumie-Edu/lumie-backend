package com.lumie.ai.application.tool.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneralQueryToolDefinitionTest {

    @Test
    void textbookGuidanceUsesCurrentFileMetadata() {
        GeneralQueryTool tool = new GeneralQueryTool(null, null, null);
        Map<?, ?> properties = (Map<?, ?>) tool.getDefinition().parameters().get("properties");
        Map<?, ?> sql = (Map<?, ?>) properties.get("sql");
        String description = (String) sql.get("description");

        assertTrue(description.contains("file_metadata"));
        assertTrue(description.contains("entity_type = 'TEXTBOOK'"));
        assertTrue(description.contains("upload_completed = true"));
        assertFalse(description.contains("textbooks"));
    }
}
