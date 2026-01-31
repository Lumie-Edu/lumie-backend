package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.TextbookResponse;
import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.TextbookRepository;
import com.lumie.content.domain.vo.TextbookStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TextbookQueryService {

    private final TextbookRepository textbookRepository;

    public Page<TextbookResponse> listTextbooks(Pageable pageable) {
        log.debug("Listing textbooks with pagination");

        return textbookRepository.findAll(pageable)
                .map(TextbookResponse::from);
    }

    public Page<TextbookResponse> listTextbooksBySubject(String subject, Pageable pageable) {
        log.debug("Listing textbooks by subject: {}", subject);

        return textbookRepository.findBySubject(subject, pageable)
                .map(TextbookResponse::from);
    }

    public List<TextbookResponse> listActiveTextbooks() {
        log.debug("Listing active textbooks");

        return textbookRepository.findByStatus(TextbookStatus.ACTIVE).stream()
                .map(TextbookResponse::from)
                .toList();
    }

    public TextbookResponse getTextbook(Long id) {
        log.debug("Getting textbook: {}", id);

        Textbook textbook = textbookRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEXTBOOK_NOT_FOUND));

        return TextbookResponse.from(textbook);
    }
}
