package com.lumie.staff.application.dto.request;

import com.lumie.common.validation.MobilePhone;
import com.lumie.common.validation.ProvidedPersonName;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record StaffUpdateRequest(
    @ProvidedPersonName String name,
    @MobilePhone String phone,
    @Email @Size(max = 255) String email,
    String staffMemo
) {
}
