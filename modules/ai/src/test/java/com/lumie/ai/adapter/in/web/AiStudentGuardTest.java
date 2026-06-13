package com.lumie.ai.adapter.in.web;

import com.lumie.ai.adapter.out.external.ChatbotClient;
import com.lumie.ai.application.service.ChatService;
import com.lumie.ai.application.service.ConversationQueryService;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI 학생 접근 guard 테스트")
class AiStudentGuardTest {

    @Mock private ChatService chatService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private ChatbotClient chatbotClient;
    @Mock private ConversationQueryService conversationQueryService;

    private MockMvc chatMvc;
    private MockMvc conversationMvc;

    @BeforeEach
    void setUp() {
        chatMvc = MockMvcBuilders
                .standaloneSetup(new ChatController(chatService, idempotencyService, chatbotClient))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        conversationMvc = MockMvcBuilders
                .standaloneSetup(new ConversationController(conversationQueryService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UserContextHolder.setUserRole(Role.STUDENT);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("학생은 채팅 생성 API를 호출할 수 없다")
    void chat_forbiddenForStudent() throws Exception {
        chatMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(chatbotClient);
    }

    @Test
    @DisplayName("학생은 대화 목록 API를 호출할 수 없다")
    void conversations_forbiddenForStudent() throws Exception {
        conversationMvc.perform(get("/v1/conversations"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversationQueryService);
    }
}
