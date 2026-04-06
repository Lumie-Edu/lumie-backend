package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.vo.SmsSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SmsMessageRepositoryAdapter implements SmsMessageRepository {

    private final JpaSmsMessageRepository jpaRepository;

    @Override
    public SmsMessage save(SmsMessage message) {
        return jpaRepository.save(message);
    }

    @Override
    public Optional<SmsMessage> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<SmsMessage> findAll(SmsSearchCriteria criteria, Pageable pageable) {
        Specification<SmsMessage> spec = toSpecification(criteria);
        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    public Page<SmsMessage> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaRepository.findByAcademyId(academyId, pageable);
    }

    private Specification<SmsMessage> toSpecification(SmsSearchCriteria criteria) {
        Specification<SmsMessage> spec = Specification.where(null);

        if (criteria.academyId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("academyId"), criteria.academyId()));
        }
        if (criteria.status() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.recipientType() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("recipientType"), criteria.recipientType()));
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + criteria.search().toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("content")), "%" + criteria.search().toLowerCase() + "%")
                ));
        }

        return spec;
    }
}
