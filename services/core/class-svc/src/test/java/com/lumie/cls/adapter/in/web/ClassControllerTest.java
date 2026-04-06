package com.lumie.cls.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.application.dto.CreateClassRequest;
import com.lumie.cls.application.dto.UpdateClassRequest;
import com.lumie.cls.application.service.ClassCommandService;
import com.lumie.cls.application.service.ClassQueryService;
import com.lumie.cls.domain.exception.ClassNotFoundException;
import com.lumie.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassController 테스트")
class ClassControllerTest {

    @Mock private ClassCommandService commandService;
    @Mock private ClassQueryService queryService;
    @InjectMocks private ClassController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("POST /v1/classes - 수업 생성 시 201을 반환한다")
    void createClass_returns201() throws Exception {
        ClassResponse response = buildClassResponse(1L);
        given(commandService.createClass(any(CreateClassRequest.class))).willReturn(response);

        CreateClassRequest request = new CreateClassRequest(
                "수학 기초반", 1L, "수학", 10L, null, "설명");

        mockMvc.perform(post("/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("수학 기초반"));
    }

    @Test
    @DisplayName("GET /v1/classes - 수업 목록 조회 시 200을 반환한다")
    void listClasses_returns200() throws Exception {
        ClassResponse response = buildClassResponse(1L);
        Page<ClassResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.listClasses(any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/v1/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("수학 기초반"));
    }

    @Test
    @DisplayName("GET /v1/classes/{id} - 수업 단건 조회 시 200을 반환한다")
    void getClass_returns200() throws Exception {
        ClassResponse response = buildClassResponse(1L);
        given(queryService.getClass(1L)).willReturn(response);

        mockMvc.perform(get("/v1/classes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("수학 기초반"));
    }

    @Test
    @DisplayName("GET /v1/classes/{id} - 존재하지 않는 수업 조회 시 404를 반환한다")
    void getClass_notFound_returns404() throws Exception {
        given(queryService.getClass(999L)).willThrow(new ClassNotFoundException(999L));

        mockMvc.perform(get("/v1/classes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /v1/classes/{id} - 수업 수정 시 200을 반환한다")
    void updateClass_returns200() throws Exception {
        ClassResponse response = buildClassResponse(1L);
        given(commandService.updateClass(eq(1L), any(UpdateClassRequest.class))).willReturn(response);

        UpdateClassRequest request = new UpdateClassRequest(
                "변경된 이름", null, null, null, null);

        mockMvc.perform(patch("/v1/classes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /v1/classes/{id} - 수업 삭제 시 204를 반환한다")
    void deleteClass_returns204() throws Exception {
        doNothing().when(commandService).deleteClass(1L);

        mockMvc.perform(delete("/v1/classes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /v1/classes/{id} - 존재하지 않는 수업 삭제 시 404를 반환한다")
    void deleteClass_notFound_returns404() throws Exception {
        doThrow(new ClassNotFoundException(999L)).when(commandService).deleteClass(999L);

        mockMvc.perform(delete("/v1/classes/999"))
                .andExpect(status().isNotFound());
    }

    private ClassResponse buildClassResponse(Long id) {
        return new ClassResponse(
                id, 1L, "수학 기초반", "수학", 10L,
                null, "설명", 5L,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
