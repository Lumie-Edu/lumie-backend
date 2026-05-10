package com.lumie.classroom.domain.entity;

import com.lumie.classroom.domain.vo.EnrollmentStatus;
import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClassEnrollment 도메인 엔티티 테스트")
class ClassEnrollmentTest {

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("create() 호출 시 기본 상태는 ENROLLED이다")
    void create_defaultStatusIsEnrolled() {
        ClassEntity classEntity = buildClassEntity();

        ClassEnrollment enrollment = ClassEnrollment.create(classEntity, 42L);

        assertThat(enrollment.getStudentId()).isEqualTo(42L);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(enrollment.getEnrolledAt()).isNotNull();
        assertThat(enrollment.getDroppedAt()).isNull();
        assertThat(enrollment.getClassEntity()).isEqualTo(classEntity);
    }

    @Test
    @DisplayName("drop() 호출 시 상태가 DROPPED로 변경되고 droppedAt이 설정된다")
    void drop_statusChangesToDropped() {
        ClassEnrollment enrollment = ClassEnrollment.create(buildClassEntity(), 42L);

        enrollment.drop();

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(enrollment.getDroppedAt()).isNotNull();
    }

    @Test
    @DisplayName("complete() 호출 시 상태가 COMPLETED로 변경된다")
    void complete_statusChangesToCompleted() {
        ClassEnrollment enrollment = ClassEnrollment.create(buildClassEntity(), 42L);

        enrollment.complete();

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getDroppedAt()).isNull();
    }

    private ClassEntity buildClassEntity() {
        return ClassEntity.create(
                "수학 기초반", 10L,
                null, null);
    }
}
