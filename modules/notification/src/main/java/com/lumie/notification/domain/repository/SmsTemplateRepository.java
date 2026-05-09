package com.lumie.notification.domain.repository;

import com.lumie.notification.domain.entity.SmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {
}
