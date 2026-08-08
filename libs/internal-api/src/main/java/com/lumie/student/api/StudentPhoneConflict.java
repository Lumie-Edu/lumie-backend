package com.lumie.student.api;

/**
 * Shared error contract for a student phone number that cannot be reused.
 *
 * <p>Both the student and exam modules expose this condition, so its public
 * code and owner-facing message live at their internal API boundary.</p>
 */
public enum StudentPhoneConflict {

    INACTIVE_STUDENT_PHONE("STUDENT_205", "이 전화번호는 퇴원 학생 기록에 존재합니다.", 409);

    private final String code;
    private final String message;
    private final int status;

    StudentPhoneConflict(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int status() {
        return status;
    }
}
