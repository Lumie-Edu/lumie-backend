package com.lumie.exam.api;

import java.util.Map;

/** Delivered synchronously inside the result mutation transaction. */
public record ExamResultChangedEvent(Long tenantId, Long examId, Long resultId,
                                     Map<String, String> answers, Integer score,
                                     Boolean passed, Integer grade) {
}
