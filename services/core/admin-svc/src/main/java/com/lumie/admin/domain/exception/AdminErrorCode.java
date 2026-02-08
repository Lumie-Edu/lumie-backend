package com.lumie.admin.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("A500", "Invalid phone number format", 400),
    INVALID_EMAIL_FORMAT("A501", "Invalid email format", 400),
    INVALID_SCHEDULE("A502", "Invalid work schedule", 400),
    INVALID_ATTENDANCE("A503", "Invalid attendance operation", 400),
    INVALID_SALARY("A504", "Invalid salary configuration", 400),
    INVALID_PAYROLL("A505", "Invalid payroll operation", 400),
    ALREADY_CLOCKED_IN("A506", "Already clocked in today", 400),
    NOT_CLOCKED_IN("A507", "Not clocked in yet", 400),
    PAYROLL_NOT_DRAFT("A508", "Payroll is not in draft status", 400),
    PAYROLL_NOT_FINALIZED("A509", "Payroll is not finalized", 400),

    // 404 Not Found
    ADMIN_NOT_FOUND("A510", "Admin not found", 404),
    POSITION_NOT_FOUND("A511", "Position not found", 404),
    PERMISSION_NOT_FOUND("A512", "Permission not found", 404),
    WORK_SCHEDULE_NOT_FOUND("A513", "Work schedule not found", 404),
    ATTENDANCE_NOT_FOUND("A514", "Attendance not found", 404),
    SALARY_NOT_FOUND("A515", "Salary not found", 404),
    PAYROLL_NOT_FOUND("A516", "Payroll record not found", 404),
    CURRENT_SALARY_NOT_FOUND("A517", "No current effective salary found", 404),
    ACADEMY_NOT_FOUND("A518", "Academy not found", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("A520", "User login ID already exists", 409),
    DUPLICATE_POSITION_NAME("A521", "Position name already exists", 409),
    DUPLICATE_PERMISSION_CODE("A522", "Permission code already exists", 409),
    DUPLICATE_SCHEDULE("A523", "Schedule already exists for this day", 409),
    DUPLICATE_ATTENDANCE("A524", "Attendance already recorded for this date", 409),
    DUPLICATE_PAYROLL("A525", "Payroll already exists for this month", 409),
    POSITION_IN_USE("A526", "해당 직책을 사용 중인 직원이 있어 삭제할 수 없습니다.", 409),
    OVERLAPPING_SALARY("A527", "Overlapping salary period exists", 409),
    ADMIN_ALREADY_ACTIVE("A528", "Admin is already active", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("A530", "Plan quota exceeded", 403),
    TENANT_INVALID("A531", "Invalid tenant", 403),
    INSUFFICIENT_PERMISSION("A532", "Insufficient permission", 403);

    private final String code;
    private final String message;
    private final int status;
}
