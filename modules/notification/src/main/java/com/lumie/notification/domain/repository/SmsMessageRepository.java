package com.lumie.notification.domain.repository;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.vo.SmsSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long>,
        JpaSpecificationExecutor<SmsMessage> {

    Optional<SmsMessage> findByIdAndSenderId(Long id, Long senderId);

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
