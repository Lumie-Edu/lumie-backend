package com.lumie.notification.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.admin.AdminService;
import com.lumie.internal.cls.ClassService;
import com.lumie.internal.student.StudentService;
import com.lumie.notification.application.dto.*;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.notification.domain.exception.SmsTemplateNotFoundException;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
import com.lumie.notification.domain.vo.TemplateCategory;
import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsCommandService 테스트")
class SmsCommandServiceTest {

    @Mock private SmsMessageRepository messageRepository;
    @Mock private SmsTemplateRepository templateRepository;
    @Mock private StudentService studentService;
    @Mock private AdminService adminService;
    @Mock private ClassService classService;
    @InjectMocks private SmsCommandService commandService;

    @org.junit.jupiter.api.BeforeEach
    void setupTenant() {
        TenantContextHolder.setTenant("test-tenant");
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("sendSms")
    class SendSms {

        @Test
        @DisplayName("정상 발송 시 SmsMessageResponse를 반환한다")
        void sendSms_success() {
            SendSmsRequest request = new SendSmsRequest(
                    10L, RecipientType.STUDENT,
                    List.of(new Recipient("010-1234-5678", "홍길동", 100L)),
                    "제목", "내용", null);

            given(adminService.getAdmin(anyString(), eq(10L)))
                    .willReturn(Optional.of(new AdminService.AdminData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendSms(request);

            assertThat(response.status()).isEqualTo(SmsStatus.SENT);
            assertThat(response.successCount()).isEqualTo(1);
            assertThat(response.recipientType()).isEqualTo(RecipientType.STUDENT);
        }

        @Test
        @DisplayName("빈 수신자 목록으로 발송 시 BusinessException이 발생한다")
        void sendSms_emptyRecipients_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    10L, RecipientType.STUDENT,
                    List.of(), "제목", "내용", null);

            given(adminService.getAdmin(anyString(), eq(10L)))
                    .willReturn(Optional.of(new AdminService.AdminData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 발송자로 발송 시 BusinessException이 발생한다")
        void sendSms_senderNotFound_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    999L, RecipientType.STUDENT,
                    List.of(new Recipient("010-1234-5678", "홍길동", 100L)),
                    "제목", "내용", null);

            given(adminService.getAdmin(anyString(), eq(999L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("sendToClass")
    class SendToClass {

        @Test
        @DisplayName("반 전체 발송 시 성공한다")
        void sendToClass_success() {
            SendToClassRequest request = new SendToClassRequest(10L, "제목", "내용", null);

            given(adminService.getAdmin(anyString(), eq(10L)))
                    .willReturn(Optional.of(new AdminService.AdminData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(classService.getClass(anyString(), eq(5L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            5L, "수학반", null, null, null, null, null, null, null)));
            given(classService.getEnrolledStudentIds(anyString(), eq(5L)))
                    .willReturn(List.of(100L, 200L));
            given(studentService.getStudentsByIds(anyString(), eq(List.of(100L, 200L))))
                    .willReturn(new StudentService.StudentListData(
                            List.of(
                                    new StudentService.StudentData(100L, 1000L, "홍길동", "010-1111-1111", true, null, null),
                                    new StudentService.StudentData(200L, 2000L, "김철수", "010-2222-2222", true, null, null)
                            ), 2L, 0, 2));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendToClass(5L, request);

            assertThat(response.status()).isEqualTo(SmsStatus.SENT);
            assertThat(response.successCount()).isEqualTo(2);
            assertThat(response.recipientType()).isEqualTo(RecipientType.STUDENT);
        }

        @Test
        @DisplayName("수강생이 없는 반에 발송 시 BusinessException이 발생한다")
        void sendToClass_noStudents_throwsException() {
            SendToClassRequest request = new SendToClassRequest(10L, "제목", "내용", null);

            given(adminService.getAdmin(anyString(), eq(10L)))
                    .willReturn(Optional.of(new AdminService.AdminData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(classService.getClass(anyString(), eq(5L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            5L, "수학반", null, null, null, null, null, null, null)));
            given(classService.getEnrolledStudentIds(anyString(), eq(5L)))
                    .willReturn(List.of());

            assertThatThrownBy(() -> commandService.sendToClass(5L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("createTemplate")
    class CreateTemplate {

        @Test
        @DisplayName("정상 생성 시 SmsTemplateResponse를 반환한다")
        void createTemplate_success() {
            CreateTemplateRequest request = new CreateTemplateRequest(
                    "출석 알림", "{학생명}님이 출석했습니다.", TemplateCategory.ATTENDANCE);

            given(templateRepository.save(any(SmsTemplate.class)))
                    .willAnswer(inv -> {
                        SmsTemplate t = inv.getArgument(0);
                        setField(t, "id", 1L);
                        return t;
                    });

            SmsTemplateResponse response = commandService.createTemplate(request);

            assertThat(response.name()).isEqualTo("출석 알림");
            assertThat(response.category()).isEqualTo(TemplateCategory.ATTENDANCE);
        }
    }

    @Nested
    @DisplayName("updateTemplate")
    class UpdateTemplate {

        @Test
        @DisplayName("정상 업데이트 시 변경된 SmsTemplateResponse를 반환한다")
        void updateTemplate_success() throws Exception {
            SmsTemplate template = SmsTemplate.create("원래", "원래 내용", TemplateCategory.GENERAL);
            setField(template, "id", 1L);

            given(templateRepository.findById(1L)).willReturn(Optional.of(template));
            given(templateRepository.save(any(SmsTemplate.class))).willAnswer(inv -> inv.getArgument(0));

            UpdateTemplateRequest request = new UpdateTemplateRequest("변경", null, TemplateCategory.EXAM);
            SmsTemplateResponse response = commandService.updateTemplate(1L, request);

            assertThat(response.name()).isEqualTo("변경");
            assertThat(response.content()).isEqualTo("원래 내용");
            assertThat(response.category()).isEqualTo(TemplateCategory.EXAM);
        }

        @Test
        @DisplayName("존재하지 않는 템플릿 업데이트 시 SmsTemplateNotFoundException이 발생한다")
        void updateTemplate_notFound_throwsException() {
            given(templateRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.updateTemplate(999L,
                    new UpdateTemplateRequest(null, null, null)))
                    .isInstanceOf(SmsTemplateNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    class DeleteTemplate {

        @Test
        @DisplayName("정상 삭제 시 성공한다")
        void deleteTemplate_success() throws Exception {
            SmsTemplate template = SmsTemplate.create("이름", "내용", TemplateCategory.GENERAL);
            setField(template, "id", 1L);
            given(templateRepository.findById(1L)).willReturn(Optional.of(template));

            commandService.deleteTemplate(1L);

            verify(templateRepository).deleteById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 템플릿 삭제 시 SmsTemplateNotFoundException이 발생한다")
        void deleteTemplate_notFound_throwsException() {
            given(templateRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.deleteTemplate(999L))
                    .isInstanceOf(SmsTemplateNotFoundException.class);
        }
    }

    // -- helpers --

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
