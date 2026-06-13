package com.lumie.content.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.repository.AnnouncementClassTargetRepository;
import com.lumie.content.domain.repository.AnnouncementRepository;
import com.lumie.file.api.FileService;
import com.lumie.staff.api.StaffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementCommandService 테스트")
class AnnouncementCommandServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementClassTargetRepository classTargetRepository;
    @Mock private FileService fileService;
    @Mock private ClassService classService;
    @Mock private StaffService staffService;
    @InjectMocks private AnnouncementCommandService commandService;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenant("test-tenant");
    }

    @AfterEach
    void tearDownTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("생성 응답은 작성자 이름을 포함한다")
    void createAnnouncement_resolvesAuthorName() {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                900L,
                "공지",
                "내용",
                false,
                false,
                null,
                List.of()
        );
        given(announcementRepository.save(any(Announcement.class))).willAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            ReflectionTestUtils.setField(announcement, "id", 1L);
            return announcement;
        });
        given(staffService.getStaffByUserId("test-tenant", 900L))
                .willReturn(Optional.of(staff(900L, "원장")));

        var response = commandService.createAnnouncement(request);

        assertThat(response.authorName()).isEqualTo("원장");
    }

    @Test
    @DisplayName("생성 응답에서 작성자 직원을 찾을 수 없으면 '삭제된 직원'으로 반환한다")
    void createAnnouncement_deletedAuthorName() {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                900L,
                "공지",
                "내용",
                false,
                false,
                null,
                List.of()
        );
        given(announcementRepository.save(any(Announcement.class))).willAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            ReflectionTestUtils.setField(announcement, "id", 1L);
            return announcement;
        });
        given(staffService.getStaffByUserId("test-tenant", 900L)).willReturn(Optional.empty());

        var response = commandService.createAnnouncement(request);

        assertThat(response.authorName()).isEqualTo("삭제된 직원");
    }

    private static StaffService.StaffData staff(Long userId, String name) {
        return new StaffService.StaffData(10L, userId, "owner", name, null, null, true, "ACTIVE", null, null);
    }
}
