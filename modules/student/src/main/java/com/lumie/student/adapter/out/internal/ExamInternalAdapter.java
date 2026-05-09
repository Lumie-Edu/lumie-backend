package com.lumie.student.adapter.out.internal;

import com.lumie.student.application.port.out.ExamServicePort;
import com.lumie.internal.exam.ExamInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("studentExamInternalAdapter")
@RequiredArgsConstructor
public class ExamInternalAdapter implements ExamServicePort {

    private final ExamInternalService examInternalService;

    @Override
    public int linkUnregisteredResultsByPhone(String phoneNumber, Long studentId) {
        return examInternalService.linkUnregisteredResultsByPhone(phoneNumber, studentId);
    }
}
