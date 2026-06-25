package com.lumie.notification.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.notification.application.dto.request.CreateSmsRecipientGroupRequest;
import com.lumie.notification.application.dto.request.RecipientRequest;
import com.lumie.notification.application.dto.response.SmsRecipientGroupResponse;
import com.lumie.notification.application.dto.response.SmsRecipientGroupSummaryResponse;
import com.lumie.notification.application.port.out.SmsRecipientGroupPersistencePort;
import com.lumie.notification.domain.entity.SmsRecipientGroup;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.staff.api.StaffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsRecipientGroupService 테스트")
class SmsRecipientGroupServiceTest {

    @Mock private SmsRecipientGroupPersistencePort groupPersistencePort;
    @Mock private StaffService staffService;
    @InjectMocks private SmsRecipientGroupService service;

    @BeforeEach
    void setupTenant() {
        TenantContextHolder.setTenant("test-tenant");
        UserContextHolder.setUserId(100L);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("수신 그룹 생성 시 현재 staff 소유로 저장하고 전화번호를 정규화한다")
    void createGroup_success() throws Exception {
        given(staffService.getStaffByUserId(anyString(), eq(100L)))
                .willReturn(Optional.of(new StaffService.StaffData(
                        10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
        given(groupPersistencePort.save(any(SmsRecipientGroup.class))).willAnswer(inv -> {
            SmsRecipientGroup group = inv.getArgument(0);
            setField(group, "id", 1L);
            return group;
        });

        CreateSmsRecipientGroupRequest request = new CreateSmsRecipientGroupRequest(
                " 전체 학부모 ",
                "mic",
                List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L)));

        SmsRecipientGroupResponse response = service.createGroup(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("전체 학부모");
        assertThat(response.recipients()).singleElement()
                .satisfies(recipient -> assertThat(recipient.phone()).isEqualTo("010-****-5678"));

        ArgumentCaptor<SmsRecipientGroup> captor = ArgumentCaptor.forClass(SmsRecipientGroup.class);
        verify(groupPersistencePort).save(captor.capture());
        assertThat(captor.getValue().getOwnerStaffId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("수신 그룹 목록은 현재 staff 소유 그룹 요약만 조회한다")
    void listGroupSummaries_scopesByCurrentStaff() {
        given(staffService.getStaffByUserId(anyString(), eq(100L)))
                .willReturn(Optional.of(new StaffService.StaffData(
                        10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
        given(groupPersistencePort.findSummariesByOwnerStaffId(10L)).willReturn(List.of(
                new SmsRecipientGroupPersistencePort.SmsRecipientGroupSummaryData(
                        1L,
                        "전체 학부모",
                        "mic",
                        1,
                        null,
                        null
                )
        ));

        List<SmsRecipientGroupSummaryResponse> response = service.listGroupSummaries();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("전체 학부모");
        assertThat(response.get(0).recipientCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("수신 그룹 상세 조회는 현재 staff 소유 범위로 제한한다")
    void getGroup_scopesByCurrentStaff() throws Exception {
        given(staffService.getStaffByUserId(anyString(), eq(100L)))
                .willReturn(Optional.of(new StaffService.StaffData(
                        10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
        SmsRecipientGroup group = SmsRecipientGroup.create(
                10L,
                "전체 학부모",
                "mic",
                List.of(Recipient.pending("01012345678", "홍길동", 100L, null, null)));
        setField(group, "id", 1L);
        given(groupPersistencePort.findByIdAndOwnerStaffId(1L, 10L)).willReturn(Optional.of(group));

        SmsRecipientGroupResponse response = service.getGroup(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.recipients()).singleElement()
                .satisfies(recipient -> assertThat(recipient.phone()).isEqualTo("010-****-5678"));
    }

    @Test
    @DisplayName("다른 staff의 수신 그룹 상세는 존재 여부를 노출하지 않는다")
    void getGroup_notOwned_throwsNotFound() {
        given(staffService.getStaffByUserId(anyString(), eq(100L)))
                .willReturn(Optional.of(new StaffService.StaffData(
                        10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
        given(groupPersistencePort.findByIdAndOwnerStaffId(1L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGroup(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("유효하지 않은 전화번호가 있으면 생성하지 않는다")
    void createGroup_invalidPhone_throwsException() {
        given(staffService.getStaffByUserId(anyString(), eq(100L)))
                .willReturn(Optional.of(new StaffService.StaffData(
                        10L, 100L, "admin", "관리자", null, null, true, "ACTIVE", null, null)));
        CreateSmsRecipientGroupRequest request = new CreateSmsRecipientGroupRequest(
                "전체 학부모",
                "mic",
                List.of(new RecipientRequest("1234", "홍길동", 100L)));

        assertThatThrownBy(() -> service.createGroup(request))
                .isInstanceOf(BusinessException.class);
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
