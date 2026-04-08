package com.lumie.activitylog.domain.repository;

import com.lumie.activitylog.domain.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ActivityLogRepository {
    ActivityLog save(ActivityLog log);
    Page<ActivityLog> findAll(Specification<ActivityLog> spec, Pageable pageable);
}
