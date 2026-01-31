package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.CreateTextbookRequest;
import com.lumie.content.application.dto.request.UpdateTextbookRequest;
import com.lumie.content.application.dto.response.TextbookResponse;
import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.TextbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextbookCommandService {

    private final TextbookRepository textbookRepository;

    @Transactional
    public TextbookResponse createTextbook(CreateTextbookRequest request) {
        log.info("Creating textbook: {}", request.title());

        Textbook textbook = Textbook.create(
                request.title(),
                request.description(),
                request.category(),
                request.fileId(),
                request.fileName(),
                request.fileUrl(),
                request.fileSize(),
                request.authorId(),
                request.authorName(),
                request.isImportant()
        );

        Textbook saved = textbookRepository.save(textbook);
        log.info("Textbook created with id: {}", saved.getId());

        return TextbookResponse.from(saved);
    }

    @Transactional
    public TextbookResponse updateTextbook(Long id, UpdateTextbookRequest request) {
        log.info("Updating textbook: {}", id);

        Textbook textbook = textbookRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEXTBOOK_NOT_FOUND));

        textbook.update(
                request.title(),
                request.description(),
                request.category(),
                request.fileId(),
                request.fileName(),
                request.fileUrl(),
                request.fileSize(),
                request.isImportant()
        );

        Textbook updated = textbookRepository.save(textbook);
        log.info("Textbook updated: {}", updated.getId());

        return TextbookResponse.from(updated);
    }

    @Transactional
    public void deleteTextbook(Long id) {
        log.info("Deleting textbook: {}", id);

        if (!textbookRepository.existsById(id)) {
            throw new ContentException(ContentErrorCode.TEXTBOOK_NOT_FOUND);
        }

        textbookRepository.deleteById(id);
        log.info("Textbook deleted: {}", id);
    }
}
