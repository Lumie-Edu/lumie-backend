package com.lumie.notification.domain.repository;

import com.lumie.notification.domain.entity.SmsTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SmsTemplateRepository {

    SmsTemplate save(SmsTemplate template);

    Optional<SmsTemplate> findById(Long id);

    Page<SmsTemplate> findAll(Pageable pageable);

    void deleteById(Long id);
}
