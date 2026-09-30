package com.lumie.student.application.dto.request;

import com.lumie.common.validation.MobilePhone;
import com.lumie.common.validation.ProvidedPersonName;

public record StudentUpdateRequest(
    @ProvidedPersonName String name,
    @MobilePhone String phone,
    String studentHighschool,
    Integer studentBirthYear,
    String parentPhone,
    String studentMemo
) {
}
