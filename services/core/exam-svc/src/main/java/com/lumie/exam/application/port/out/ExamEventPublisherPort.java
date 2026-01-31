package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;

public interface ExamEventPublisherPort {

    void publishExamCreated(Exam exam, String tenantSlug);

    void publishResultSubmitted(ExamResult result, String tenantSlug);
}
