package com.lumie.staff.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class StaffBoundaryServiceTest {
    @Test
    void commandAndQueryServicesKeepTheirExistingTransactionResponsibilities() {
        assertThat(StaffCommandService.class.getAnnotation(Transactional.class)).isNotNull();
        assertThat(StaffCommandService.class.getAnnotation(Transactional.class).readOnly()).isFalse();
        assertThat(StaffQueryService.class.getAnnotation(Transactional.class)).isNotNull();
        assertThat(StaffQueryService.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(StaffStatisticsQueryService.class.getAnnotation(Transactional.class)).isNotNull();
        assertThat(StaffStatisticsQueryService.class.getAnnotation(Transactional.class).readOnly()).isTrue();
    }
}
