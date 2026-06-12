package com.lumie.content.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaDetailResponse;
import com.lumie.content.application.dto.response.QnaUnansweredOverdueResponse;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.staff.api.StaffService;
import com.lumie.student.api.StudentService;
import com.lumie.common.exception.BusinessException;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.repository.QnaBoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QnaQueryService {

    private final QnaBoardRepository qnaBoardRepository;
    private final StudentService studentService;
    private final StaffService staffService;

    public Page<QnaBoardResponse> listQnas(String keyword, Boolean isAnswered, Pageable pageable) {
        log.debug("Listing Q&As with pagination, keyword={}, isAnswered={}", keyword, isAnswered);

        List<Long> userIds = null;
        if (keyword != null && !keyword.isBlank()) {
            String tenantSlug = TenantContextHolder.getRequiredTenant();
            userIds = studentService.searchUserIds(tenantSlug, keyword);
        }

        Page<QnaBoard> page = qnaBoardRepository.search(keyword, userIds, isAnswered, pageable);

        return enrichWithUserNames(page);
    }

    public Page<QnaBoardResponse> listQnasByUser(Long qnaUserId, Pageable pageable) {
        log.debug("Listing Q&As for user: {}", qnaUserId);

        Page<QnaBoard> page = qnaBoardRepository.findByQnaUserId(qnaUserId, pageable);
        return enrichWithUserNames(page);
    }

    public Page<QnaBoardResponse> listUnansweredQnas(Pageable pageable) {
        log.debug("Listing unanswered Q&As");

        Page<QnaBoard> page = qnaBoardRepository.findByIsAnsweredFalse(pageable);
        return enrichWithUserNames(page);
    }

    @Transactional
    public QnaDetailResponse getQna(Long id) {
        log.debug("Getting Q&A detail: {}", id);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.QNA_NOT_FOUND));

        Long currentUserId = com.lumie.common.tenant.UserContextHolder.getUserId();
        if (currentUserId != null && currentUserId.equals(qnaBoard.getQnaUserId())) {
            qnaBoard.markAsReadByAuthor();
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String userName = studentService.getStudentsByUserIds(tenantSlug, List.of(qnaBoard.getQnaUserId()))
                .students()
                .stream()
                .findFirst()
                .map(StudentService.StudentData::studentName)
                .orElse("삭제된 유저");

        List<Long> userIds = qnaBoard.getComments().stream()
                .map(c -> c.getUserId())
                .filter(userId -> userId != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> adminNameMap = userIds.stream()
                .flatMap(userId -> staffService.getStaffByUserId(tenantSlug, userId)
                        .map(s -> Map.entry(userId, s.name()))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        return QnaDetailResponse.from(qnaBoard, userName, adminNameMap);
    }

    public QnaUnansweredOverdueResponse getUnansweredOverdueStatistics(long thresholdHours) {
        log.debug("Computing unanswered-overdue Q&A count, thresholdHours={}", thresholdHours);
        Instant threshold = Instant.now().minusSeconds(thresholdHours * 3600L);
        long count = qnaBoardRepository.countByIsAnsweredFalseAndCreatedAtBefore(threshold);
        return new QnaUnansweredOverdueResponse(count, thresholdHours);
    }

    public boolean canUserViewQna(Long qnaId, String role, Long userId) {
        return qnaBoardRepository.findById(qnaId)
                .map(qna -> Role.fromString(role) != Role.STUDENT || qna.getQnaUserId().equals(userId))
                .orElse(false);
    }

    private Page<QnaBoardResponse> enrichWithUserNames(Page<QnaBoard> page) {
        List<Long> userIds = page.getContent().stream()
                .map(QnaBoard::getQnaUserId)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return page.map(qna -> QnaBoardResponse.from(qna, null));
        }

        log.info("[QNA] Looking up names for userIds: {}", userIds);

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        var students = studentService.getStudentsByUserIds(tenantSlug, userIds).students();
        log.info("[QNA] Found {} students", students.size());
        students.forEach(s -> log.info("[QNA] student userId={} name={}", s.userId(), s.studentName()));

        Map<Long, String> nameMap = students.stream()
                .collect(Collectors.toMap(
                        StudentService.StudentData::userId,
                        StudentService.StudentData::studentName,
                        (a, b) -> a
                ));

        return page.map(qna -> QnaBoardResponse.from(
                qna,
                nameMap.getOrDefault(qna.getQnaUserId(), "삭제된 유저")
        ));
    }
}
