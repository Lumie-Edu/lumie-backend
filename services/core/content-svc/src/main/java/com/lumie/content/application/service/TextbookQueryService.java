package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.TextbookResponse;
import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.TextbookRepository;
import com.lumie.content.domain.vo.TextbookCategory;
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

    public Page<TextbookResponse> listTextbooksByCategory(TextbookCategory category, Pageable pageable) {
        log.debug("Listing textbooks by category: {}", category);

        return textbookRepository.findByCategory(category, pageable)
                .map(TextbookResponse::from);
    }

    public List<TextbookResponse> listImportantTextbooks() {
        log.debug("Listing important textbooks");

        return textbookRepository.findByIsImportantTrue().stream()
                .map(TextbookResponse::from)
                .toList();
    }

    public TextbookResponse getTextbook(Long id) {
        log.debug("Getting textbook: {}", id);

        Textbook textbook = textbookRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEXTBOOK_NOT_FOUND));

        return TextbookResponse.from(textbook);
    }

    @Transactional
    public TextbookResponse downloadTextbook(Long id) {
        log.debug("Downloading textbook: {}", id);

        Textbook textbook = textbookRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEXTBOOK_NOT_FOUND));

        textbook.incrementDownloadCount();
        textbookRepository.save(textbook);

        return TextbookResponse.from(textbook);
    }
}
