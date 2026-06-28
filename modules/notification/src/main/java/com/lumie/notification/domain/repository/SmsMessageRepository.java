package com.lumie.notification.domain.repository;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.vo.SmsSearchCriteria;
import com.lumie.notification.domain.vo.SmsStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long>,
        JpaSpecificationExecutor<SmsMessage> {

    Optional<SmsMessage> findByIdAndSenderId(Long id, Long senderId);

    Optional<SmsMessage> findByProviderGroupId(String providerGroupId);

    @Query("""
            select m.id
            from SmsMessage m
            where m.status = :status
              and (m.scheduledAt is null or m.scheduledAt <= :now)
              and coalesce(m.dispatchAttemptCount, 0) < :maxAttempts
              and (
                    m.lastProviderStatus is null
                    or m.lastProviderStatus <> 'DISPATCHING'
                    or m.updatedAt <= :claimBefore
                  )
            order by coalesce(m.scheduledAt, m.createdAt) asc, m.id asc
            """)
    List<Long> findDueDispatchIds(
            @Param("status") SmsStatus status,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            @Param("claimBefore") Instant claimBefore,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SmsMessage m
            set m.provider = :provider,
                m.dispatchAttemptCount = coalesce(m.dispatchAttemptCount, 0) + 1,
                m.lastProviderStatus = 'DISPATCHING',
                m.lastProviderError = null,
                m.updatedAt = :now,
                m.version = m.version + 1
            where m.id = :id
              and m.status = :status
              and coalesce(m.dispatchAttemptCount, 0) < :maxAttempts
              and (
                    m.lastProviderStatus is null
                    or m.lastProviderStatus <> 'DISPATCHING'
                    or m.updatedAt <= :claimBefore
                  )
            """)
    int claimDispatchAttempt(
            @Param("id") Long id,
            @Param("status") SmsStatus status,
            @Param("provider") String provider,
            @Param("maxAttempts") int maxAttempts,
            @Param("claimBefore") Instant claimBefore,
            @Param("now") Instant now
    );

    default Page<SmsMessage> findAll(SmsSearchCriteria criteria, Pageable pageable) {
        Specification<SmsMessage> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.senderId() != null) {
                predicates.add(cb.equal(root.get("senderId"), criteria.senderId()));
            }
            if (criteria.recipientGroupId() != null) {
                predicates.add(cb.equal(root.get("recipientGroupId"), criteria.recipientGroupId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.recipientType() != null) {
                predicates.add(cb.equal(root.get("recipientType"), criteria.recipientType()));
            }
            if (criteria.search() != null && !criteria.search().isBlank()) {
                String pattern = "%" + criteria.search().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
