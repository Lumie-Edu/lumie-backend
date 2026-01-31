package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.QnaBoardResponse;
import com.lumie.content.application.dto.response.QnaDetailResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QnaQueryService {

    private final QnaBoardRepository qnaBoardRepository;

    public Page<QnaBoardResponse> listQnas(Pageable pageable) {
        log.debug("Listing Q&As with pagination");

        return qnaBoardRepository.findAll(pageable)
                .map(QnaBoardResponse::from);
    }

    public Page<QnaBoardResponse> listQnasByStudent(Long studentId, Pageable pageable) {
        log.debug("Listing Q&As for student: {}", studentId);

        return qnaBoardRepository.findByStudentId(studentId, pageable)
                .map(QnaBoardResponse::from);
    }

    public Page<QnaBoardResponse> listUnansweredQnas(Pageable pageable) {
        log.debug("Listing unanswered Q&As");

        return qnaBoardRepository.findByIsAnsweredFalse(pageable)
                .map(QnaBoardResponse::from);
    }

    @Transactional
    public QnaDetailResponse getQna(Long id) {
        log.debug("Getting Q&A detail: {}", id);

        QnaBoard qnaBoard = qnaBoardRepository.findByIdWithComments(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.QNA_NOT_FOUND));

        qnaBoard.incrementViewCount();
        qnaBoardRepository.save(qnaBoard);

        return QnaDetailResponse.from(qnaBoard);
    }
}
