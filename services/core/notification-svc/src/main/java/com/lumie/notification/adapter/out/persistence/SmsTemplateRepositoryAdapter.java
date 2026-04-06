package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SmsTemplateRepositoryAdapter implements SmsTemplateRepository {

    private final JpaSmsTemplateRepository jpaRepository;

    @Override
    public SmsTemplate save(SmsTemplate template) {
        return jpaRepository.save(template);
    }

    @Override
    public Optional<SmsTemplate> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<SmsTemplate> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaRepository.findByAcademyId(academyId, pageable);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
