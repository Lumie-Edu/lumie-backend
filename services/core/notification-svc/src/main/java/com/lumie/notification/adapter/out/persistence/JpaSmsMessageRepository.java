package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.domain.entity.SmsMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaSmsMessageRepository extends JpaRepository<SmsMessage, Long>, JpaSpecificationExecutor<SmsMessage> {

    Page<SmsMessage> findByAcademyId(Long academyId, Pageable pageable);
}
