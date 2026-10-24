package com.lumie.notification.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.auth.Role;
import com.lumie.staff.api.StaffService;
import com.lumie.classroom.api.ClassService;
import com.lumie.student.api.StudentService;
import com.lumie.notification.application.dto.request.RecipientRequest;
import com.lumie.notification.application.dto.request.SendSmsRequest;
import com.lumie.notification.application.dto.request.SendToClassRequest;
import com.lumie.notification.application.dto.request.CreateTemplateRequest;
import com.lumie.notification.application.dto.request.UpdateTemplateRequest;
import com.lumie.notification.application.port.out.SmsRecipientGroupPersistencePort;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.dto.response.SmsTemplateResponse;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.entity.SmsRecipientGroup;
import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.common.exception.BusinessException;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientRole;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
import com.lumie.notification.domain.vo.TemplateCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsCommandService 테스트")
class SmsCommandServiceTest {

    @Mock private SmsMessageRepository messageRepository;
    @Mock private SmsTemplateRepository templateRepository;
    @Mock private StudentService studentService;
    @Mock private StaffService staffService;
    @Mock private ClassService classService;
    @Mock private SmsRecipientGroupPersistencePort recipientGroupPersistencePort;
    @Mock private SmsSenderSettingService senderSettingService;
    @Mock private SmsProviderRegistry providerRegistry;
    @Mock private SmsDispatchService dispatchService;
    @InjectMocks private SmsCommandService commandService;

    @org.junit.jupiter.api.BeforeEach
    void setupTenant() {
        TenantContextHolder.setTenant("test-tenant");
        UserContextHolder.setUserId(100L);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        lenient().when(providerRegistry.isMockProvider()).thenReturn(true);
        lenient().when(senderSettingService.requireApprovedSenderNumber()).thenReturn("0212345678");
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Nested
    @DisplayName("sendSms")
    class SendSms {

        @Test
        @DisplayName("정상 발송 시 SmsMessageResponse를 반환한다")
        void sendSms_success() {
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.STUDENT)),
                    "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendSms(request);

            assertThat(response.status()).isEqualTo(SmsStatus.PENDING);
            assertThat(response.successCount()).isZero();
            assertThat(response.recipientType()).isEqualTo(RecipientType.STUDENT);
            ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().getSenderNumber()).isNull();
            verify(dispatchService).dispatchAfterCommit(1L);
        }

        @Test
        @DisplayName("SOLAPI provider는 승인된 발신 번호를 저장한다")
        void sendSms_solapiProvider_storesApprovedSenderNumber() {
            given(providerRegistry.isMockProvider()).willReturn(false);
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.STUDENT)),
                    "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            commandService.sendSms(request);

            ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().getSenderNumber()).isEqualTo("0212345678");
        }

        @Test
        @DisplayName("학생과 학부모를 함께 직접 발송하면 CUSTOM recipientType으로 저장한다")
        void sendSms_inlineMixedRecipients_infersCustomRecipientType() {
            SendSmsRequest request = new SendSmsRequest(
                    List.of(
                            new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.STUDENT),
                            new RecipientRequest("010-9876-5432", "김철수", 200L, null, RecipientRole.GUARDIAN)
                    ),
                    "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendSms(request);

            assertThat(response.recipientType()).isEqualTo(RecipientType.CUSTOM);
            assertThat(response.recipients()).hasSize(2);
            verify(dispatchService).dispatchAfterCommit(1L);
        }

        @Test
        @DisplayName("빈 수신자 목록으로 발송 시 BusinessException이 발생한다")
        void sendSms_emptyRecipients_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    List.of(), "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 발송자로 발송 시 BusinessException이 발생한다")
        void sendSms_senderNotFound_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                    "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("recipientGroupId가 있으면 서버에서 현재 staff 소유 그룹으로 수신자를 로드한다")
        void sendSms_withRecipientGroupId_usesStoredRecipients() throws Exception {
            SendSmsRequest request = new SendSmsRequest(
                    null,
                    "제목",
                    55L,
                    "{학생명} 안내",
                    null,
                    null
            );
            SmsRecipientGroup group = SmsRecipientGroup.create(
                    10L,
                    "학부모 그룹",
                    "mic",
                    List.of(Recipient.pending("01012345678", "홍길동", 100L, "수학반", RecipientRole.GUARDIAN, null))
            );

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(recipientGroupPersistencePort.findByIdAndOwnerStaffId(55L, 10L))
                    .willReturn(Optional.of(group));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendSms(request);

            assertThat(response.recipientGroupId()).isEqualTo(55L);
            assertThat(response.recipients()).singleElement().satisfies(recipient -> {
                assertThat(recipient.phone()).isEqualTo("010-****-5678");
                assertThat(recipient.name()).isEqualTo("홍길동");
            });
            verify(recipientGroupPersistencePort).findByIdAndOwnerStaffId(55L, 10L);
            verify(dispatchService).dispatchAfterCommit(1L);
        }

        @Test
        @DisplayName("recipientGroupId가 현재 staff 소유 범위에 없으면 발송하지 않는다")
        void sendSms_withUnknownRecipientGroup_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    null,
                    "제목",
                    55L,
                    "내용",
                    null,
                    null
            );

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(recipientGroupPersistencePort.findByIdAndOwnerStaffId(55L, 10L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("그룹 발송에 inline recipients를 함께 보내면 거절한다")
        void sendSms_groupSendWithInlineRecipients_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("010-9999-9999", "무시됨", 999L, null, RecipientRole.GUARDIAN)),
                    "제목",
                    55L,
                    "내용",
                    null,
                    null
            );

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
            verify(recipientGroupPersistencePort, never()).findByIdAndOwnerStaffId(any(), any());
        }

        @Test
        @DisplayName("그룹 발송은 저장된 수신자 역할로 recipientType을 추론한다")
        void sendSms_groupSendInfersRecipientTypeFromStoredRecipients() throws Exception {
            SendSmsRequest request = new SendSmsRequest(
                    null,
                    "제목",
                    55L,
                    "내용",
                    null,
                    null
            );
            SmsRecipientGroup group = SmsRecipientGroup.create(
                    10L,
                    "혼합 그룹",
                    "mic",
                    List.of(
                            Recipient.pending("01012345678", "홍길동", 100L, "수학반", RecipientRole.STUDENT, null),
                            Recipient.pending("01098765432", "김철수", 200L, "수학반", RecipientRole.GUARDIAN, null)
                    )
            );

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(recipientGroupPersistencePort.findByIdAndOwnerStaffId(55L, 10L))
                    .willReturn(Optional.of(group));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendSms(request);

            assertThat(response.recipientType()).isEqualTo(RecipientType.CUSTOM);
            assertThat(response.recipients()).hasSize(2);
            verify(recipientGroupPersistencePort).findByIdAndOwnerStaffId(55L, 10L);
            verify(dispatchService).dispatchAfterCommit(1L);
        }

        @Test
        @DisplayName("미래 예약 발송은 대기 상태로 저장한다")
        void sendSms_futureSchedule_persistsPending() {
            Instant scheduledAt = Instant.now().plusSeconds(3600);
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                    "제목", "{학생명} 안내", null, scheduledAt);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendSms(request);

            assertThat(response.status()).isEqualTo(SmsStatus.PENDING);
            assertThat(response.scheduledAt()).isEqualTo(scheduledAt);
            assertThat(response.recipients()).singleElement()
                    .satisfies(recipient -> assertThat(recipient.name()).isEqualTo("홍길동"));
            verify(dispatchService, never()).dispatchAfterCommit(any());
        }

        @Test
        @DisplayName("과거 예약 시간으로 발송 시 BusinessException이 발생한다")
        void sendSms_pastSchedule_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                    "제목", "내용", null, Instant.now().minusSeconds(60));

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("유효하지 않은 수신자 번호로 발송 시 BusinessException이 발생한다")
        void sendSms_invalidRecipientPhone_throwsException() {
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("1234", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                    "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("SOLAPI provider에서 승인된 발신 번호가 없으면 발송하지 않는다")
        void sendSms_solapiProviderWithoutApprovedSenderNumber_throwsException() {
            given(providerRegistry.isMockProvider()).willReturn(false);
            SendSmsRequest request = new SendSmsRequest(
                    List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                    "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(senderSettingService.requireApprovedSenderNumber())
                    .willThrow(new BusinessException(com.lumie.notification.domain.exception.SmsErrorCode.SENDER_SETTING_NOT_APPROVED));

            assertThatThrownBy(() -> commandService.sendSms(request))
                    .isInstanceOf(BusinessException.class);
            verify(messageRepository, never()).save(any(SmsMessage.class));
        }
    }

    @Nested
    @DisplayName("sendToClass")
    class SendToClass {

        @Test
        @DisplayName("반 전체 발송 시 성공한다")
        void sendToClass_success() {
            SendToClassRequest request = new SendToClassRequest(RecipientRole.GUARDIAN, "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(classService.getClass(anyString(), eq(5L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            5L, "수학반", null, null, null, null)));
            given(classService.getEnrolledStudentIds(anyString(), eq(5L)))
                    .willReturn(List.of(100L, 200L));
            given(studentService.getStudentsByIds(anyString(), eq(List.of(100L, 200L))))
                    .willReturn(new StudentService.StudentListData(
                            List.of(
                                    new StudentService.StudentData(100L, 1000L, "홍길동", "010-1111-1111", "010-3333-3333", true, null, null),
                                    new StudentService.StudentData(200L, 2000L, "김철수", "010-2222-2222", "010-4444-4444", true, null, null)
                            ), 2L, 0, 2));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendToClass(5L, request);

            assertThat(response.status()).isEqualTo(SmsStatus.PENDING);
            assertThat(response.successCount()).isZero();
            assertThat(response.recipientType()).isEqualTo(RecipientType.GUARDIAN);
            verify(dispatchService).dispatchAfterCommit(1L);
        }

        @Test
        @DisplayName("반 전체 학생 발송 시 학생 전화번호로 발송한다")
        void sendToClass_studentRecipients_success() {
            SendToClassRequest request = new SendToClassRequest(RecipientRole.STUDENT, "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(classService.getClass(anyString(), eq(5L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            5L, "수학반", null, null, null, null)));
            given(classService.getEnrolledStudentIds(anyString(), eq(5L)))
                    .willReturn(List.of(100L));
            given(studentService.getStudentsByIds(anyString(), eq(List.of(100L))))
                    .willReturn(new StudentService.StudentListData(
                            List.of(new StudentService.StudentData(
                                    100L, 1000L, "홍길동", "010-1111-1111", "010-3333-3333", true, null, null)),
                            1L, 0, 1));
            given(messageRepository.save(any(SmsMessage.class)))
                    .willAnswer(inv -> {
                        SmsMessage msg = inv.getArgument(0);
                        setField(msg, "id", 1L);
                        return msg;
                    });

            SmsMessageResponse response = commandService.sendToClass(5L, request);

            assertThat(response.recipientType()).isEqualTo(RecipientType.STUDENT);
            assertThat(response.recipients()).singleElement().satisfies(recipient -> {
                assertThat(recipient.recipientRole()).isEqualTo(RecipientRole.STUDENT);
                assertThat(recipient.phone()).isEqualTo("010-****-1111");
            });
            verify(dispatchService).dispatchAfterCommit(1L);
        }

        @Test
        @DisplayName("수강생이 없는 반에 발송 시 BusinessException이 발생한다")
        void sendToClass_noStudents_throwsException() {
            SendToClassRequest request = new SendToClassRequest(RecipientRole.GUARDIAN, "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(classService.getClass(anyString(), eq(5L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            5L, "수학반", null, null, null, null)));
            given(classService.getEnrolledStudentIds(anyString(), eq(5L)))
                    .willReturn(List.of());

            assertThatThrownBy(() -> commandService.sendToClass(5L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("학부모 전화번호가 없는 반 발송 시 BusinessException이 발생한다")
        void sendToClass_missingParentPhone_throwsException() {
            SendToClassRequest request = new SendToClassRequest(RecipientRole.GUARDIAN, "제목", "내용", null);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(classService.getClass(anyString(), eq(5L)))
                    .willReturn(Optional.of(new ClassService.ClassData(
                            5L, "수학반", null, null, null, null)));
            given(classService.getEnrolledStudentIds(anyString(), eq(5L)))
                    .willReturn(List.of(100L));
            given(studentService.getStudentsByIds(anyString(), eq(List.of(100L))))
                    .willReturn(new StudentService.StudentListData(
                            List.of(new StudentService.StudentData(
                                    100L, 1000L, "홍길동", "010-1111-1111", null, true, null, null)),
                            1L, 0, 1));

            assertThatThrownBy(() -> commandService.sendToClass(5L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("cancelScheduled")
    class CancelScheduled {

        @Test
        @DisplayName("예약 대기 문자를 취소한다")
        void cancelScheduled_success() throws Exception {
            Instant scheduledAt = Instant.now().plusSeconds(3600);
            SmsMessage message = SmsMessage.create(
                    10L,
                    RecipientType.GUARDIAN,
                    List.of(com.lumie.notification.domain.vo.Recipient.pending(
                            "01012345678", "홍길동", 100L, null, RecipientRole.GUARDIAN, "내용")),
                    "제목",
                    "내용",
                    null,
                    scheduledAt,
                    com.lumie.notification.domain.vo.SmsMessageType.SMS,
                    1,
                    20,
                    20);
            setField(message, "id", 1L);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.findByIdAndSenderId(1L, 10L)).willReturn(Optional.of(message));
            given(messageRepository.save(any(SmsMessage.class))).willAnswer(inv -> inv.getArgument(0));

            SmsMessageResponse response = commandService.cancelScheduled(1L);

            assertThat(response.status()).isEqualTo(SmsStatus.CANCELLED);
            assertThat(response.cancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 발송된 문자는 취소할 수 없다")
        void cancelScheduled_sentMessage_throwsException() throws Exception {
            SmsMessage message = SmsMessage.create(
                    10L,
                    RecipientType.GUARDIAN,
                    List.of(com.lumie.notification.domain.vo.Recipient.pending(
                            "01012345678", "홍길동", 100L, null, RecipientRole.GUARDIAN, "내용")),
                    "제목",
                    "내용",
                    null,
                    null,
                    com.lumie.notification.domain.vo.SmsMessageType.SMS,
                    1,
                    20,
                    20);
            setField(message, "id", 1L);
            message.markAsSent();

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.findByIdAndSenderId(1L, 10L)).willReturn(Optional.of(message));

            assertThatThrownBy(() -> commandService.cancelScheduled(1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("이미 취소된 예약 문자는 현재 상태를 그대로 반환한다")
        void cancelScheduled_alreadyCancelled_returnsExistingState() throws Exception {
            Instant scheduledAt = Instant.now().plusSeconds(3600);
            SmsMessage message = SmsMessage.create(
                    10L,
                    RecipientType.GUARDIAN,
                    List.of(com.lumie.notification.domain.vo.Recipient.pending(
                            "01012345678", "홍길동", 100L, null, RecipientRole.GUARDIAN, "내용")),
                    "제목",
                    "내용",
                    null,
                    scheduledAt,
                    com.lumie.notification.domain.vo.SmsMessageType.SMS,
                    1,
                    20,
                    20);
            setField(message, "id", 1L);
            message.cancel();

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.findByIdAndSenderId(1L, 10L)).willReturn(Optional.of(message));

            SmsMessageResponse response = commandService.cancelScheduled(1L);

            assertThat(response.status()).isEqualTo(SmsStatus.CANCELLED);
            assertThat(response.cancelledAt()).isEqualTo(message.getCancelledAt());
            verify(messageRepository, never()).save(any(SmsMessage.class));
        }

        @Test
        @DisplayName("강사는 다른 직원의 예약 문자를 취소할 수 없다")
        void cancelScheduled_otherSender_throwsException() throws Exception {
            SmsMessage message = SmsMessage.create(
                    20L,
                    RecipientType.GUARDIAN,
                    List.of(com.lumie.notification.domain.vo.Recipient.pending(
                            "01012345678", "홍길동", 100L, null, RecipientRole.GUARDIAN, "내용")),
                    "제목",
                    "내용",
                    null,
                    Instant.now().plusSeconds(3600),
                    com.lumie.notification.domain.vo.SmsMessageType.SMS,
                    1,
                    20,
                    20);
            setField(message, "id", 1L);

            given(staffService.getStaffByUserId(anyString(), eq(100L)))
                    .willReturn(Optional.of(new StaffService.StaffData(
                            10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
            given(messageRepository.findByIdAndSenderId(1L, 10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.cancelScheduled(1L))
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
                    "시험 알림", "시험 결과가 등록되었습니다.", TemplateCategory.EXAM);

            given(templateRepository.save(any(SmsTemplate.class)))
                    .willAnswer(inv -> {
                        SmsTemplate t = inv.getArgument(0);
                        setField(t, "id", 1L);
                        return t;
                    });

            SmsTemplateResponse response = commandService.createTemplate(request);

            assertThat(response.name()).isEqualTo("시험 알림");
            assertThat(response.category()).isEqualTo(TemplateCategory.EXAM);
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
                    .isInstanceOf(BusinessException.class);
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
                    .isInstanceOf(BusinessException.class);
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
