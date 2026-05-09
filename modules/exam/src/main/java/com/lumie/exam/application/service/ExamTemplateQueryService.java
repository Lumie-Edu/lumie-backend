package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.ExamTemplateResponse;
import com.lumie.exam.domain.entity.ExamTemplate;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamTemplateRepository;
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
public class ExamTemplateQueryService {

    private final ExamTemplateRepository examTemplateRepository;

    public ExamTemplateResponse getTemplate(Long id) {
        log.debug("Getting exam template: {}", id);

        ExamTemplate template = examTemplateRepository.findById(id)
                .orElseThrow(() -> new ExamException(ExamErrorCode.TEMPLATE_NOT_FOUND));

        return ExamTemplateResponse.from(template);
    }

    public Page<ExamTemplateResponse> listTemplates(Pageable pageable) {
        log.debug("Listing exam templates with pagination");

        return examTemplateRepository.findAll(pageable)
                .map(ExamTemplateResponse::from);
    }
}
