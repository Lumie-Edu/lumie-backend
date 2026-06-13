package com.lumie.content.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.repository.AnnouncementClassTargetRepository;
import com.lumie.content.domain.repository.AnnouncementReadRepository;
import com.lumie.content.domain.repository.AnnouncementRepository;
import com.lumie.staff.api.StaffService;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementQueryService 테스트")
class AnnouncementQueryServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementClassTargetRepository classTargetRepository;
    @Mock private AnnouncementReadRepository announcementReadRepository;
    @Mock private StaffService staffService;
    @Mock private StudentService studentService;
    @Mock private ClassService classService;
    @InjectMocks private AnnouncementQueryService queryService;

    @BeforeEach
    void setUpContext() {
        TenantContextHolder.setTenant("test-tenant");
        UserContextHolder.setUserRole(Role.OWNER);
    }

    @AfterEach
    void tearDownContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("삭제된 직원 작성자는 '삭제된 직원'으로 반환한다")
    void listAnnouncements_deletedAuthorName() {
        Announcement announcement = announcement(1L, 900L);
        given(announcementRepository.findAllOrderByImportantDescCreatedAtDesc(PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(announcement), PageRequest.of(0, 20), 1));
        given(classTargetRepository.findByAnnouncementIdIn(List.of(1L))).willReturn(List.of());
        given(staffService.getStaffByUserId("test-tenant", 900L)).willReturn(Optional.empty());

        var response = queryService.listAnnouncements(null, PageRequest.of(0, 20));

        assertThat(response.getContent().get(0).authorName()).isEqualTo("삭제된 직원");
    }

    @Test
    @DisplayName("읽은 학생을 찾을 수 없으면 '삭제된 학생'으로 반환한다")
    void getAnnouncementReaders_deletedStudentName() {
        Announcement announcement = announcement(1L, 900L);
        given(announcementRepository.findById(1L)).willReturn(Optional.of(announcement));
        given(announcementReadRepository.findUserIdsByAnnouncementId(1L)).willReturn(List.of(100L));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(new StudentService.StudentListData(List.of(), 0, 0, 0));

        List<String> readers = queryService.getAnnouncementReaders(1L);

        assertThat(readers).containsExactly("삭제된 학생");
    }

    private static Announcement announcement(Long id, Long authorId) {
        Announcement announcement = Announcement.create(authorId, "공지", "내용", false, false);
        ReflectionTestUtils.setField(announcement, "id", id);
        return announcement;
    }
}
