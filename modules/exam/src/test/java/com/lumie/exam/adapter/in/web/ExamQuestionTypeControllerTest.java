package com.lumie.exam.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.port.out.ExamQuestionTypePort;
import com.lumie.exam.application.service.ExamQuestionTypeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExamQuestionTypeControllerTest {

    @Mock
    private ExamQuestionTypePort examQuestionTypePort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExamQuestionTypeService service = new ExamQuestionTypeService(examQuestionTypePort);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExamQuestionTypeController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UserContextHolder.setUserRole(Role.STUDENT);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void createQuestionTypeRejectsStudentUser() throws Exception {
        mockMvc.perform(post("/v1/exam-question-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"장문독해\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(examQuestionTypePort);
    }

    @Test
    void deleteQuestionTypeRejectsStudentUser() throws Exception {
        mockMvc.perform(delete("/v1/exam-question-types/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(examQuestionTypePort);
    }

}
