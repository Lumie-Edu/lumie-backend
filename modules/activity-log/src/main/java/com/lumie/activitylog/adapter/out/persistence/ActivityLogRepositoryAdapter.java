package com.lumie.activitylog.adapter.out.persistence;

import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ActivityLogRepositoryAdapter implements ActivityLogRepository {

    private final JpaActivityLogRepository jpaActivityLogRepository;

    @Override
    public ActivityLog save(ActivityLog log) {
        return jpaActivityLogRepository.save(log);
    }

    @Override
    public Page<ActivityLog> findAll(Specification<ActivityLog> spec, Pageable pageable) {
        return jpaActivityLogRepository.findAll(spec, pageable);
    }
}
