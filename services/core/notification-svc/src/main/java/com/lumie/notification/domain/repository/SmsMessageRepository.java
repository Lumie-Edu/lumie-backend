package com.lumie.notification.domain.repository;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.vo.SmsSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SmsMessageRepository {

    SmsMessage save(SmsMessage message);

    Optional<SmsMessage> findById(Long id);

    Page<SmsMessage> findAll(SmsSearchCriteria criteria, Pageable pageable);
}
