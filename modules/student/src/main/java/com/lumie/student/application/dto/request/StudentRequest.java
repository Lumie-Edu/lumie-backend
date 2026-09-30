package com.lumie.student.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.lumie.common.validation.PersonName;
import com.lumie.common.validation.PersonNamePolicy;
import com.lumie.common.validation.MobilePhone;

public record StudentRequest(
    @PersonName
        @Schema(minLength = PersonNamePolicy.MIN_LENGTH, maxLength = PersonNamePolicy.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @MobilePhone
    String phone,

    String studentHighschool,
    Integer studentBirthYear,
    String parentPhone,
    String studentMemo
) {
}
