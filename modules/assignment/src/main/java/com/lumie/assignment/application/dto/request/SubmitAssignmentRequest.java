package com.lumie.assignment.application.dto.request;

import java.util.List;
import java.util.Map;

public record SubmitAssignmentRequest(
    String content,
    List<Long> attachments,
    Map<String, String> answers
) {
}
