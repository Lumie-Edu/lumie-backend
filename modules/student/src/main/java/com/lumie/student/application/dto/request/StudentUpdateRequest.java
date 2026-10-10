package com.lumie.student.application.dto.request;

import com.lumie.common.validation.MobilePhone;
import com.lumie.common.validation.ProvidedPersonName;
import com.lumie.common.validation.LoginIdPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record StudentUpdateRequest(
    @ProvidedPersonName String name,
    @MobilePhone String phone,
    String studentHighschool,
    Integer studentBirthYear,
    String parentPhone,
    String studentMemo,
    @Size(min = LoginIdPolicy.MIN_LENGTH, max = LoginIdPolicy.MAX_LENGTH)
    @Pattern(regexp = LoginIdPolicy.PATTERN)
    @Schema(minLength = LoginIdPolicy.MIN_LENGTH, maxLength = LoginIdPolicy.MAX_LENGTH,
            pattern = LoginIdPolicy.PATTERN, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String loginId,
    List<@NotNull Long> classIds
) {
    public StudentUpdateRequest(
            String name,
            String phone,
            String studentHighschool,
            Integer studentBirthYear,
            String parentPhone,
            String studentMemo
    ) {
        this(name, phone, studentHighschool, studentBirthYear, parentPhone, studentMemo, null, null);
    }
}
