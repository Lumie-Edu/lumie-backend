package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.PaddleWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaddleWebhookEventJpaRepository extends JpaRepository<PaddleWebhookEvent, String> {
}
