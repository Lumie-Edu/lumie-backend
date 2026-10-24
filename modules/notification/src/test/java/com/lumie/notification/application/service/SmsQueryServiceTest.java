package com.lumie.notification.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.dto.response.SmsTemplateResponse;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.common.exception.BusinessException;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import com.lumie.notification.domain.vo.*;
import com.lumie.staff.api.StaffService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsQueryService 테스트")
class SmsQueryServiceTest {

    @Mock private SmsMessageRepository messageRepository;
    @Mock private SmsTemplateRepository templateRepository;
    @Mock private StaffService staffService;
    @InjectMocks private SmsQueryService queryService;

    @BeforeEach
    void setupTenant() {
        TenantContextHolder.setTenant("test-tenant");
        UserContextHolder.setUserId(100L);
        UserContextHolder.setUserRole(Role.OWNER);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("getMessage 성공 시 SmsMessageResponse를 반환한다")
    void getMessage_success() throws Exception {
        SmsMessage message = buildSmsMessage(1L);
        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        SmsMessageResponse response = queryService.getMessage(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("테스트 내용");
    }

    @Test
    @DisplayName("getMessage 존재하지 않으면 SmsMessageNotFoundException이 발생한다")
    void getMessage_notFound_throwsException() {
        given(messageRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getMessage(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("listMessages 성공 시 페이지네이션된 결과를 반환한다")
    void listMessages_success() throws Exception {
        SmsMessage message = buildSmsMessage(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<SmsMessage> page = new PageImpl<>(List.of(message), pageable, 1);

        given(messageRepository.findAll(any(SmsSearchCriteria.class), eq(pageable))).willReturn(page);

        Page<SmsMessageResponse> response = queryService.listMessages(null, null, null, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).id()).isEqualTo(1L);
        assertThat(response.getContent().get(0).content()).isEqualTo("테스트 내용");
    }

    @Test
    @DisplayName("listMessages는 긴 본문도 반환한다")
    void listMessages_returnsLongContentPreview() throws Exception {
        String longContent = "가".repeat(130);
        SmsMessage message = buildSmsMessage(1L, longContent);
        Pageable pageable = PageRequest.of(0, 20);
        Page<SmsMessage> page = new PageImpl<>(List.of(message), pageable, 1);

        given(messageRepository.findAll(any(SmsSearchCriteria.class), eq(pageable))).willReturn(page);

        Page<SmsMessageResponse> response = queryService.listMessages(null, null, null, null, pageable);

        assertThat(response.getContent().get(0).content()).isEqualTo(longContent);
    }

    @Test
    @DisplayName("listMessages는 매우 짧은 본문도 반환한다")
    void listMessages_returnsVeryShortContentPreview() throws Exception {
        SmsMessage message = buildSmsMessage(1L, "네");
        Pageable pageable = PageRequest.of(0, 20);
        Page<SmsMessage> page = new PageImpl<>(List.of(message), pageable, 1);

        given(messageRepository.findAll(any(SmsSearchCriteria.class), eq(pageable))).willReturn(page);

        Page<SmsMessageResponse> response = queryService.listMessages(null, null, null, null, pageable);

        assertThat(response.getContent().get(0).content()).isEqualTo("네");
    }

    @Test
    @DisplayName("listMessages 필터 조건을 SmsSearchCriteria로 전달한다")
    void listMessages_withFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SmsMessage> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(messageRepository.findAll(any(SmsSearchCriteria.class), eq(pageable))).willReturn(emptyPage);

        Page<SmsMessageResponse> response = queryService.listMessages(
            SmsStatus.SENT, RecipientType.STUDENT, "검색어", 99L, pageable);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("강사는 본인 senderId로 문자 이력을 제한한다")
    void listMessages_instructor_scopesBySenderId() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        Pageable pageable = PageRequest.of(0, 20);
        Page<SmsMessage> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(staffService.getStaffByUserId("test-tenant", 100L))
                .willReturn(Optional.of(new StaffService.StaffData(
                        10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
        given(messageRepository.findAll(any(SmsSearchCriteria.class), eq(pageable))).willReturn(emptyPage);

        queryService.listMessages(null, null, null, null, pageable);

        verify(messageRepository).findAll(eq(new SmsSearchCriteria(null, null, null, null, 10L)), eq(pageable));
    }

    @Test
    @DisplayName("강사는 다른 senderId의 문자 상세를 조회하지 못한다")
    void getMessage_instructor_scopesBySenderId() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        given(staffService.getStaffByUserId("test-tenant", 100L))
                .willReturn(Optional.of(new StaffService.StaffData(
                        10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
        given(messageRepository.findByIdAndSenderId(1L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getMessage(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("listTemplates 성공 시 페이지네이션된 결과를 반환한다")
    void listTemplates_success() throws Exception {
        SmsTemplate template = SmsTemplate.create("시험 알림", "내용", TemplateCategory.EXAM);
        setField(template, "id", 1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<SmsTemplate> page = new PageImpl<>(List.of(template), pageable, 1);

        given(templateRepository.findAll(pageable)).willReturn(page);

        Page<SmsTemplateResponse> response = queryService.listTemplates(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).name()).isEqualTo("시험 알림");
    }

    // -- helpers --

    private SmsMessage buildSmsMessage(Long id) throws Exception {
        return buildSmsMessage(id, "테스트 내용");
    }

    private SmsMessage buildSmsMessage(Long id, String content) throws Exception {
        SmsMessage message = SmsMessage.create(
                10L, RecipientType.PARENT,
                List.of(Recipient.pending("010-1234-5678", "홍길동", 100L, null, RecipientRole.PARENT, null)),
                "테스트 제목", content, null);
        setField(message, "id", id);
        return message;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}
