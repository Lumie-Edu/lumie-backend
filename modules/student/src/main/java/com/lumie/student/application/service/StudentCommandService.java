package com.lumie.student.application.service;

import com.lumie.student.application.dto.request.StudentRequest;
import com.lumie.student.application.dto.request.StudentUpdateRequest;
import com.lumie.student.application.dto.response.BatchOperationResult;
import com.lumie.student.application.dto.response.BulkImportResult;
import com.lumie.student.application.dto.response.StudentResponse;
import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.student.api.StudentRegisteredEvent;
import com.lumie.student.domain.entity.Student;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.quota.MetricType;
import com.lumie.common.exception.QuotaExceededException;
import com.lumie.student.domain.exception.StudentErrorCode;
import com.lumie.student.domain.repository.StudentRepository;
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.common.validation.PersonNamePolicy;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentCommandService {

    private static final int FINGERPRINT_BUFFER_SIZE = 8_192;

    private final StudentRepository studentRepository;
    private final AuthService authService;
    private final BillingService billingService;
    private final ClassService classService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public StudentResponse registerStudent(StudentRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        checkStudentQuota(tenantSlug);

        validateStudentPhoneFormat(request.phone());
        validatePhoneUniqueness(request.phone(), null);
        validateParentPhoneFormat(request.parentPhone());

        AuthService.CreateUserResult userResult = authService.createUser(
                request.name(),
                request.phone(),
                Role.STUDENT.name(),
                tenantId
        );

        if (!userResult.success()) {
            throw new BusinessException(StudentErrorCode.AUTH_OP_FAILED, "Failed to create user: " + userResult.message());
        }

        Student student = Student.create(
            userResult.userId(),
            userResult.userLoginId(),
            request.name(),
            request.phone(),
            request.studentHighschool(),
            request.studentBirthYear(),
            request.parentPhone(),
            request.studentMemo()
        );

        Student saved = studentRepository.save(student);

        // Submission reconciliation runs as an AFTER_COMMIT listener to keep the
        // student and exam modules decoupled.
        eventPublisher.publishEvent(new StudentRegisteredEvent(
                saved.getId(),
                saved.getPhone(),
                tenantSlug,
                TenantContextHolder.getRequiredTenantId()
        ));

        log.info("Student registered: {} in tenant: {}", saved.getUserLoginId(), tenantSlug);
        return StudentResponse.from(saved);
    }

    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with id: %d".formatted(id)));

        validateStudentPhoneFormat(request.phone());
        validatePhoneUniqueness(request.phone(), id);
        validateParentPhoneFormat(request.parentPhone());

        student.updateInfo(
            request.name(),
            request.phone(),
            request.studentHighschool(),
            request.studentBirthYear(),
            request.parentPhone(),
            request.studentMemo()
        );

        Student updated = studentRepository.save(student);
        if (request.loginId() != null && !request.loginId().isBlank()
                && !request.loginId().equals(student.getUserLoginId())) {
            changeStudentLoginId(id, request.loginId());
        }
        if (request.classIds() != null) {
            classService.replaceStudentEnrollments(tenantSlug, id, request.classIds());
        }
        if ((request.name() != null || request.phone() != null)
                && !authService.updateIdentity(student.getUserId(), request.name(),
                        PhoneNumberUtils.toDigits(request.phone()), null)) {
            throw new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Linked user not found");
        }

        log.info("Student updated: {} in tenant: {}", id, tenantSlug);
        return StudentResponse.from(updated);
    }

    public void deactivateStudent(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with id: %d".formatted(id)));

        student.deactivate();
        studentRepository.save(student);

        log.info("Student deactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void reactivateStudent(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        checkStudentQuota(tenantSlug);

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with id: %d".formatted(id)));

        student.activate();
        studentRepository.save(student);

        log.info("Student reactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void deleteStudent(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with id: %d".formatted(id)));

        if (student.isActive()) {
            throw new BusinessException(StudentErrorCode.CANNOT_DELETE_ACTIVE_STUDENT);
        }
        classService.dropActiveEnrollmentsForStudent(tenantSlug, id);

        Long userId = student.getUserId();
        // Detach so the FK CASCADE from auth-svc deletion does not race the JPA dirty-check.
        entityManager.detach(student);

        // Delete user from auth-svc (FK CASCADE automatically deletes student)
        AuthService.DeleteUserResult deleteResult = authService.deleteUser(userId);
        if (!deleteResult.success()) {
            log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
        }

        log.info("Student permanently deleted: {} in tenant: {}", id, tenantSlug);
    }

    public void resetStudentPassword(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with id: %d".formatted(id)));

        AuthService.ResetPasswordResult result = authService.resetPassword(student.getUserId());
        if (!result.success()) {
            throw new BusinessException(StudentErrorCode.AUTH_OP_FAILED, "Failed to reset password: " + result.message());
        }

        log.info("Password reset for student: {} in tenant: {}", id, tenantSlug);
    }

    public String changeStudentLoginId(Long id, String newLoginId) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with id: %d".formatted(id)));

        AuthService.ChangeLoginIdResult result = authService.changeLoginId(student.getUserId(), newLoginId);
        if (!result.success()) {
            throw new BusinessException(StudentErrorCode.AUTH_OP_FAILED, "Failed to change login ID: " + result.message());
        }

        student.updateLoginId(result.newLoginId());
        studentRepository.save(student);

        log.info("Login ID changed for student: {} in tenant: {}", id, tenantSlug);
        return result.newLoginId();
    }

    public BatchOperationResult batchDeactivate(List<Long> ids) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        List<BatchOperationResult.FailedItem> failures = new ArrayList<>();

        List<Student> students = studentRepository.findAllById(ids);
        Map<Long, Student> studentMap = students.stream()
                .collect(java.util.stream.Collectors.toMap(Student::getId, s -> s));

        List<Student> toDeactivate = new ArrayList<>();
        for (Long id : ids) {
            Student student = studentMap.get(id);
            if (student == null) {
                failures.add(new BatchOperationResult.FailedItem(id, "Student not found"));
            } else if (!student.isActive()) {
                failures.add(new BatchOperationResult.FailedItem(id, "Already deactivated"));
            } else {
                student.deactivate();
                toDeactivate.add(student);
            }
        }

        if (!toDeactivate.isEmpty()) {
            studentRepository.saveAll(toDeactivate);
        }

        int successCount = toDeactivate.size();
        log.info("Batch deactivate completed: {} success, {} failures in tenant: {}", successCount, failures.size(), tenantSlug);
        return BatchOperationResult.partial(ids.size(), successCount, failures);
    }

    public BatchOperationResult batchReactivate(List<Long> ids) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        List<BatchOperationResult.FailedItem> failures = new ArrayList<>();

        List<Student> students = studentRepository.findAllById(ids);
        Map<Long, Student> studentMap = students.stream()
                .collect(java.util.stream.Collectors.toMap(Student::getId, s -> s));

        List<Student> toReactivate = new ArrayList<>();
        for (Long id : ids) {
            Student student = studentMap.get(id);
            if (student == null) {
                failures.add(new BatchOperationResult.FailedItem(id, "Student not found"));
            } else if (student.isActive()) {
                failures.add(new BatchOperationResult.FailedItem(id, "Already active"));
            } else {
                student.activate();
                toReactivate.add(student);
            }
        }

        if (!toReactivate.isEmpty()) {
            studentRepository.saveAll(toReactivate);
        }

        int successCount = toReactivate.size();
        log.info("Batch reactivate completed: {} success, {} failures in tenant: {}", successCount, failures.size(), tenantSlug);
        return BatchOperationResult.partial(ids.size(), successCount, failures);
    }

    public BatchOperationResult batchDelete(List<Long> ids) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        List<BatchOperationResult.FailedItem> failures = new ArrayList<>();

        List<Student> students = studentRepository.findAllById(ids);
        Map<Long, Student> studentMap = students.stream()
                .collect(java.util.stream.Collectors.toMap(Student::getId, s -> s));

        List<Student> toDelete = new ArrayList<>();
        for (Long id : ids) {
            Student student = studentMap.get(id);
            if (student == null) {
                failures.add(new BatchOperationResult.FailedItem(id, "Student not found"));
            } else if (student.isActive()) {
                failures.add(new BatchOperationResult.FailedItem(id, "Cannot delete active student"));
            } else {
                toDelete.add(student);
            }
        }

        // Detach students from persistence context before user deletion
        toDelete.forEach(entityManager::detach);

        // Delete users from auth-svc (FK CASCADE automatically deletes students)
        for (Student student : toDelete) {
            AuthService.DeleteUserResult deleteResult = authService.deleteUser(student.getUserId());
            if (!deleteResult.success()) {
                log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
            }
        }

        int successCount = toDelete.size();
        log.info("Batch delete completed: {} success, {} failures in tenant: {}", successCount, failures.size(), tenantSlug);
        return BatchOperationResult.partial(ids.size(), successCount, failures);
    }

    public String hashBulkImportFile(MultipartFile file) {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[FINGERPRINT_BUFFER_SIZE];

        try (InputStream input = file.getInputStream()) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            log.error("Failed to fingerprint student import file", e);
            throw new BusinessException(StudentErrorCode.EXCEL_PARSE_FAILED, "Failed to read student import file: " + e.getMessage());
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    public BulkImportResult bulkImportStudents(MultipartFile file) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        List<StudentExcelParser.StudentRow> rows;
        try {
            rows = StudentExcelParser.parse(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            log.error("Failed to read student import file", e);
            throw new BusinessException(StudentErrorCode.EXCEL_PARSE_FAILED, "Failed to read student import file: " + e.getMessage());
        }

        // ── Pass 1: validate all rows upfront, collecting every error ──────────
        // Phone conflicts are classified against the DB state and duplicate rows in the
        // same file are reported before any auth user or student row is created.
        List<BulkImportResult.RowError> errors = new ArrayList<>();
        Set<String> seenPhones = new HashSet<>();
        for (StudentExcelParser.StudentRow row : rows) {
            validateBulkRow(row, errors, seenPhones);
        }
        Set<Integer> rowsWithValidationErrors = errors.stream()
                .map(BulkImportResult.RowError::rowNumber)
                .collect(Collectors.toSet());

        // ── Pass 2: insert only the rows that passed validation ───────────────
        int successCount = 0;
        boolean quotaExhausted = false;

        for (StudentExcelParser.StudentRow row : rows) {
            if (rowsWithValidationErrors.contains(row.rowNumber())) continue;

            if (quotaExhausted) {
                errors.add(rowError(row.rowNumber(), "quota", null,
                        StudentErrorCode.BULK_IMPORT_QUOTA_EXCEEDED));
                continue;
            }

            try {
                checkStudentQuota(tenantSlug);

                AuthService.CreateUserResult userResult = authService.createUser(
                        row.name(),
                        row.phone(),
                        Role.STUDENT.name(),
                        tenantId
                );

                if (!userResult.success()) {
                    errors.add(rowError(row.rowNumber(), null, null,
                            StudentErrorCode.BULK_IMPORT_AUTH_FAILED,
                            StudentErrorCode.BULK_IMPORT_AUTH_FAILED.getMessage()
                                    + " (" + userResult.message() + ")"));
                    continue;
                }

                Student student = Student.create(
                        userResult.userId(),
                        userResult.userLoginId(),
                        row.name(),
                        row.phone(),
                        row.studentHighschool(),
                        row.studentBirthYear(),
                        row.parentPhone(),
                        row.studentMemo()
                );
                Student saved = studentRepository.save(student);
                eventPublisher.publishEvent(new StudentRegisteredEvent(
                        saved.getId(),
                        saved.getPhone(),
                        tenantSlug,
                        tenantId
                ));
                successCount++;

            } catch (QuotaExceededException e) {
                quotaExhausted = true;
                errors.add(rowError(row.rowNumber(), "quota", null,
                        StudentErrorCode.BULK_IMPORT_QUOTA_EXCEEDED));
            } catch (Exception e) {
                errors.add(rowError(row.rowNumber(), null, null,
                        StudentErrorCode.BULK_IMPORT_UNEXPECTED, e.getMessage()));
            }
        }

        log.info("Bulk import completed: {} success, {} failures in tenant: {}",
                 successCount, errors.size(), tenantSlug);

        return BulkImportResult.partial(rows.size(), successCount, errors);
    }

    /**
     * Validates a single parsed row and appends any validation errors to {@code errors}.
     * All errors for the row are collected (no early return within the row) so the
     * teacher sees every problem at once.
     */
    private void validateBulkRow(StudentExcelParser.StudentRow row,
                                  List<BulkImportResult.RowError> errors,
                                  Set<String> seenPhones) {
        // name: required
        if (row.name() == null || row.name().isBlank()) {
            errors.add(rowError(row.rowNumber(), "name", row.name(),
                    StudentErrorCode.BULK_IMPORT_NAME_REQUIRED));
        } else if (!PersonNamePolicy.isValid(row.name())) {
            errors.add(rowError(row.rowNumber(), "name", row.name(),
                    StudentErrorCode.BULK_IMPORT_INVALID_LENGTH, "이름은 2–100자여야 합니다."));
        } else if (isUnsafeSpreadsheetValue(row.name())) {
            errors.add(rowError(row.rowNumber(), "name", row.name(),
                    StudentErrorCode.BULK_IMPORT_UNSAFE_SPREADSHEET_VALUE));
        }

        // phone: format + uniqueness (optional field — only validate when present)
        if (row.phone() != null && !row.phone().isBlank()) {
            if (!PhoneNumberUtils.isValidStudentPhone(row.phone())) {
                errors.add(rowError(row.rowNumber(), "phone", row.phone(),
                        StudentErrorCode.BULK_IMPORT_INVALID_PHONE));
            } else {
                String normalizedPhone = PhoneNumberUtils.toDigits(row.phone());
                if (!seenPhones.add(normalizedPhone)) {
                    errors.add(rowError(row.rowNumber(), "phone", row.phone(),
                            StudentErrorCode.BULK_IMPORT_DUPLICATE_PHONE));
                } else {
                    StudentPhoneConflictStatus conflict = resolveStudentPhoneConflict(normalizedPhone, null);
                    if (conflict == StudentPhoneConflictStatus.INACTIVE_STUDENT) {
                        errors.add(rowError(row.rowNumber(), "phone", row.phone(),
                                StudentErrorCode.BULK_IMPORT_INACTIVE_STUDENT_PHONE));
                    } else if (conflict == StudentPhoneConflictStatus.ACTIVE_STUDENT) {
                        errors.add(rowError(row.rowNumber(), "phone", row.phone(),
                                StudentErrorCode.BULK_IMPORT_DUPLICATE_PHONE));
                    }
                }
            }
        }

        // parentPhone: format only (no uniqueness constraint required for parent phones)
        if (row.parentPhone() != null && !row.parentPhone().isBlank()) {
            if (row.parentPhone().length() > 20) {
                errors.add(rowError(row.rowNumber(), "parentPhone", row.parentPhone(),
                        StudentErrorCode.BULK_IMPORT_INVALID_LENGTH, "학부모 연락처는 20자를 초과할 수 없습니다."));
            } else if (!PhoneNumberUtils.isValid(row.parentPhone())) {
                errors.add(rowError(row.rowNumber(), "parentPhone", row.parentPhone(),
                        StudentErrorCode.BULK_IMPORT_INVALID_PHONE));
            }
        }

        // studentHighschool
        if (row.studentHighschool() != null && row.studentHighschool().length() > 100) {
            errors.add(rowError(row.rowNumber(), "studentHighschool", row.studentHighschool(),
                    StudentErrorCode.BULK_IMPORT_INVALID_LENGTH, "학교명은 100자를 초과할 수 없습니다."));
        } else if (isUnsafeSpreadsheetValue(row.studentHighschool())) {
            errors.add(rowError(row.rowNumber(), "studentHighschool", row.studentHighschool(),
                    StudentErrorCode.BULK_IMPORT_UNSAFE_SPREADSHEET_VALUE));
        }

        if (isUnsafeSpreadsheetValue(row.studentMemo())) {
            errors.add(rowError(row.rowNumber(), "studentMemo", row.studentMemo(),
                    StudentErrorCode.BULK_IMPORT_UNSAFE_SPREADSHEET_VALUE));
        }
    }

    private static boolean isUnsafeSpreadsheetValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        char first = value.stripLeading().charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@';
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(StudentErrorCode.EXCEL_PARSE_FAILED, "Unable to fingerprint student import file");
        }
    }

    private static BulkImportResult.RowError rowError(
            int rowNumber, String column, String value, StudentErrorCode code) {
        return new BulkImportResult.RowError(rowNumber, column, value, code.getCode(), code.getMessage());
    }

    private static BulkImportResult.RowError rowError(
            int rowNumber, String column, String value, StudentErrorCode code, String overrideMessage) {
        return new BulkImportResult.RowError(rowNumber, column, value, code.getCode(), overrideMessage);
    }

    private void validatePhoneUniqueness(String phone, Long excludeId) {
        String p = PhoneNumberUtils.toDigits(phone);
        if (p == null || p.isBlank()) {
            return;
        }

        switch (resolveStudentPhoneConflict(p, excludeId)) {
            case NONE -> {
                return;
            }
            case INACTIVE_STUDENT -> throw new BusinessException(StudentErrorCode.INACTIVE_STUDENT_PHONE);
            case ACTIVE_STUDENT -> throw new BusinessException(
                    StudentErrorCode.DUPLICATE_PHONE,
                    "Phone number already exists: %s".formatted(phone));
        }
    }

    private StudentPhoneConflictStatus resolveStudentPhoneConflict(String normalizedPhone, Long excludeId) {
        boolean duplicate = excludeId == null
                ? studentRepository.existsByCanonicalPhone(normalizedPhone)
                : studentRepository.existsByCanonicalPhoneExcluding(normalizedPhone, excludeId);
        if (!duplicate) {
            return StudentPhoneConflictStatus.NONE;
        }

        return studentRepository.findByCanonicalPhone(normalizedPhone)
                .filter(existing -> excludeId == null || !java.util.Objects.equals(existing.getId(), excludeId))
                .map(existing -> existing.isActive()
                        ? StudentPhoneConflictStatus.ACTIVE_STUDENT
                        : StudentPhoneConflictStatus.INACTIVE_STUDENT)
                // A concurrent deletion or update must never allow a duplicate registration.
                .orElse(StudentPhoneConflictStatus.ACTIVE_STUDENT);
    }

    private enum StudentPhoneConflictStatus {
        NONE,
        ACTIVE_STUDENT,
        INACTIVE_STUDENT
    }

    private void validateStudentPhoneFormat(String phone) {
        if (phone != null && !phone.isBlank() && !PhoneNumberUtils.isValidStudentPhone(phone)) {
            throw new BusinessException(
                    StudentErrorCode.INVALID_PHONE_NUMBER,
                    "Invalid student phone number format: %s".formatted(phone));
        }
    }

    private void validateParentPhoneFormat(String parentPhone) {
        if (parentPhone != null && !parentPhone.isBlank() && !PhoneNumberUtils.isValid(parentPhone)) {
            throw new BusinessException(
                    StudentErrorCode.INVALID_PHONE_NUMBER,
                    "Invalid parent phone number format: %s".formatted(parentPhone));
        }
    }

    private void checkStudentQuota(String tenantSlug) {
        var result = billingService.checkQuota(tenantSlug, MetricType.STUDENTS);
        if (!result.allowed()) {
            throw new QuotaExceededException(
                StudentErrorCode.QUOTA_EXCEEDED,
                TenantContextHolder.getTenant(),
                MetricType.STUDENTS,
                result.currentUsage(),
                result.limit()
            );
        }
    }
}
