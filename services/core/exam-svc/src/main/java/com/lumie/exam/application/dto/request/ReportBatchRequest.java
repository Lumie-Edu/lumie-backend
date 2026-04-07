package com.lumie.exam.application.dto.request;

import java.util.List;

public record ReportBatchRequest(List<Long> studentIds) {
}
