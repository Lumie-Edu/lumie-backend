package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.domain.entity.SmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {
}
