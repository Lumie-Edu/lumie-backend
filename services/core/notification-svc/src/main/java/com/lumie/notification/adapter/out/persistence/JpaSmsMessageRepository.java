package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.domain.entity.SmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaSmsMessageRepository extends JpaRepository<SmsMessage, Long>, JpaSpecificationExecutor<SmsMessage> {
}
