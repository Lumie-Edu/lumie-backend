package com.lumie.content.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaDetailResponse;
import com.lumie.content.application.port.out.AdminServicePort;
import com.lumie.content.application.port.out.StudentServicePort;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.QnaBoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QnaQueryService {

    private final QnaBoardRepository qnaBoardRepository;
    private final StudentServicePort studentServicePort;
    private final AdminServicePort adminServicePort;

    public Page<QnaBoardResponse> listQnas(Boolean isItAnswered, Pageable pageable) {
        log.debug("Listing Q&As with pagination, isItAnswered={}", isItAnswered);

        Page<QnaBoard> page = isItAnswered != null
                ? qnaBoardRepository.findByIsItAnswered(isItAnswered, pageable)
                : qnaBoardRepository.findAll(pageable);

        return enrichWithUserNames(page);
    }

    public Page<QnaBoardResponse> listQnasByUser(Long qnaUserId, Pageable pageable) {
        log.debug("Listing Q&As for user: {}", qnaUserId);

        Page<QnaBoard> page = qnaBoardRepository.findByQnaUserId(qnaUserId, pageable);
        return enrichWithUserNames(page);
    }

    public Page<QnaBoardResponse> listUnansweredQnas(Pageable pageable) {
        log.debug("Listing unanswered Q&As");

        Page<QnaBoard> page = qnaBoardRepository.findByIsItAnsweredFalse(pageable);
        return enrichWithUserNames(page);
    }

    public QnaDetailResponse getQna(Long id) {
        log.debug("Getting Q&A detail: {}", id);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.QNA_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getTenant();
        String userName = studentServicePort.getStudentsByUserIds(tenantSlug, List.of(qnaBoard.getQnaUserId()))
                .stream()
                .findFirst()
                .map(StudentServicePort.StudentInfo::name)
                .orElse(String.valueOf(qnaBoard.getQnaUserId()));

        List<Long> userIds = qnaBoard.getComments().stream()
                .map(c -> c.getUserId())
                .filter(userId -> userId != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> adminNameMap = userIds.stream()
                .flatMap(userId -> adminServicePort.getAdminByUserId(tenantSlug, userId)
                        .map(a -> Map.entry(userId, a.adminName()))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        return QnaDetailResponse.from(qnaBoard, userName, adminNameMap);
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

        String tenantSlug = TenantContextHolder.getTenant();
        var students = studentServicePort.getStudentsByUserIds(tenantSlug, userIds);
        log.info("[QNA] Found {} students", students.size());
        students.forEach(s -> log.info("[QNA] student userId={} name={}", s.userId(), s.name()));

        Map<Long, String> nameMap = students.stream()
                .collect(Collectors.toMap(
                        StudentServicePort.StudentInfo::userId,
                        StudentServicePort.StudentInfo::name,
                        (a, b) -> a
                ));

        return page.map(qna -> QnaBoardResponse.from(
                qna,
                nameMap.getOrDefault(qna.getQnaUserId(), String.valueOf(qna.getQnaUserId()))
        ));
    }
}
