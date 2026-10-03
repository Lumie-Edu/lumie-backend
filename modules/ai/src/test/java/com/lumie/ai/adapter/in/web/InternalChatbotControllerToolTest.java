package com.lumie.ai.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.service.ChatService;
import com.lumie.ai.application.service.SchemaDiscoveryService;
import com.lumie.ai.application.tool.ToolRegistry;
import com.lumie.ai.application.tool.impl.GeneralQueryTool;
import com.lumie.ai.domain.exception.AiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InternalChatbotControllerToolTest {

    @Mock private GeneralQueryTool generalQueryTool;
    @Mock private ToolRegistry toolRegistry;
    @Mock private SchemaDiscoveryService schemaDiscoveryService;
    @Mock private ChatService chatService;

    @Test
    void retiredScheduledTaskCannotBeExecuted() {
        InternalChatbotController controller = new InternalChatbotController(
                generalQueryTool, toolRegistry, schemaDiscoveryService, chatService, new ObjectMapper());

        assertThrows(AiException.class, () -> controller.executeTool(
                "schedule_task",
                new InternalChatbotController.ToolExecuteRequest(
                        "joossameng", 1L, 1L, new ObjectMapper().createObjectNode())));

        verifyNoInteractions(toolRegistry);
    }
}
