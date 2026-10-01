package com.lumie.staff.domain.entity;

import com.lumie.staff.application.dto.response.StaffResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StaffCanonicalIdentityTest {
    @Test
    void userIdentityIsTheOnlySourceForStaffResponses() {
        Staff staff = Staff.create(7L, "staff_7", "옛 이름", "01011112222",
                "old@example.com", null);
        ReflectionTestUtils.setField(staff, "canonicalName", "새 이름");
        ReflectionTestUtils.setField(staff, "canonicalPhone", null);
        ReflectionTestUtils.setField(staff, "canonicalEmail", null);

        StaffResponse response = StaffResponse.from(staff);
        assertThat(response.name()).isEqualTo("새 이름");
        assertThat(response.phone()).isNull();
        assertThat(response.email()).isNull();

        staff.updateInfo("관리자 수정", "010-3333-4444", "new@example.com", null);
        assertThat(staff.getStaffName()).isEqualTo("관리자 수정");
        assertThat(staff.getStaffPhone()).isEqualTo("01033334444");
        assertThat(staff.getEmail()).isEqualTo("new@example.com");
    }
}
