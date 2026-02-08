package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.BatchOperationResult;
import com.lumie.academy.application.dto.BulkImportResult;
import com.lumie.academy.application.dto.StudentRequest;
import com.lumie.academy.application.dto.StudentResponse;
import com.lumie.academy.application.dto.StudentUpdateRequest;
import com.lumie.academy.application.port.out.AuthServicePort;
import com.lumie.academy.application.port.out.BillingServicePort;
import com.lumie.academy.application.port.out.MemberEventPublisherPort;
import com.lumie.academy.domain.entity.Academy;
import com.lumie.academy.domain.entity.Student;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.exception.DuplicateUserLoginIdException;
import com.lumie.academy.domain.exception.QuotaExceededException;
import com.lumie.academy.domain.exception.StudentNotFoundException;
import com.lumie.academy.domain.repository.AcademyRepository;
import com.lumie.academy.domain.repository.StudentRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.messaging.event.MemberCreatedEvent;
import com.lumie.messaging.event.MemberDeletedEvent;
import com.lumie.messaging.event.MemberUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentCommandService {

    private final StudentRepository studentRepository;
    private final AcademyRepository academyRepository;
    private final AuthServicePort authServicePort;
    private final BillingServicePort billingServicePort;
    private final MemberEventPublisherPort eventPublisher;

    public StudentResponse registerStudent(StudentRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        checkStudentQuota(tenantSlug);

        if (studentRepository.existsByUserLoginId(request.userLoginId())) {
            throw new DuplicateUserLoginIdException(request.userLoginId());
        }

        Academy academy = academyRepository.findById(request.academyId())
                .orElseThrow(() -> new AcademyNotFoundException(request.academyId()));

        // Create user in auth-svc first
        AuthServicePort.CreateUserResult userResult = authServicePort.createUser(
                new AuthServicePort.CreateUserRequest(
                        request.userLoginId(),
                        request.password(),
                        request.name(),
                        request.phone(),
                        "STUDENT",
                        tenantId
                )
        );

        if (!userResult.success()) {
            throw new RuntimeException("Failed to create user: " + userResult.message());
        }

        Student student = Student.create(
            userResult.userId(),
            request.userLoginId(),
            request.name(),
            request.phone(),
            academy,
            request.studentHighschool(),
            request.studentBirthYear(),
            request.parentPhone(),
            request.studentMemo()
        );

        Student saved = studentRepository.save(student);

        eventPublisher.publish(new MemberCreatedEvent(
            saved.getId(),
            tenantSlug,
            "STUDENT",
            saved.getStudentName(),
            saved.getStudentEmail(),
            academy.getId()
        ));

        log.info("Student registered: {} in tenant: {}", saved.getUserLoginId(), tenantSlug);
        return StudentResponse.from(saved);
    }

    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        student.updateInfo(
            request.name(),
            request.phone(),
            request.studentHighschool(),
            request.studentBirthYear(),
            request.parentPhone(),
            request.studentMemo()
        );

        // Handle academy change
        if (request.academyId() != null && !request.academyId().equals(student.getAcademy().getId())) {
            Academy newAcademy = academyRepository.findById(request.academyId())
                    .orElseThrow(() -> new AcademyNotFoundException(request.academyId()));
            student.changeAcademy(newAcademy);
        }

        Student updated = studentRepository.save(student);

        eventPublisher.publish(new MemberUpdatedEvent(
            updated.getId(),
            tenantSlug,
            "STUDENT",
            updated.getStudentName()
        ));

        log.info("Student updated: {} in tenant: {}", id, tenantSlug);
        return StudentResponse.from(updated);
    }

    public void deactivateStudent(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        student.deactivate();
        studentRepository.save(student);

        eventPublisher.publish(new MemberDeletedEvent(
            student.getId(),
            tenantSlug,
            "STUDENT"
        ));

        log.info("Student deactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void reactivateStudent(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        checkStudentQuota(tenantSlug);

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        student.activate();
        studentRepository.save(student);

        log.info("Student reactivated: {} in tenant: {}", id, tenantSlug);
    }

    public void deleteStudent(Long id) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        if (student.isActive()) {
            throw new IllegalStateException("Cannot delete active student. Deactivate first.");
        }

        Long userId = student.getUserId();
        studentRepository.detach(student);

        // Delete user from auth-svc (FK CASCADE automatically deletes student)
        AuthServicePort.DeleteUserResult deleteResult = authServicePort.deleteUser(userId);
        if (!deleteResult.success()) {
            log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
        }

        log.info("Student permanently deleted: {} in tenant: {}", id, tenantSlug);
    }

    public BatchOperationResult batchDeactivate(List<Long> ids) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        List<BatchOperationResult.FailedItem> failures = new ArrayList<>();

        // Batch fetch all students
        List<Student> students = studentRepository.findAllByIds(ids);
        Map<Long, Student> studentMap = students.stream()
                .collect(java.util.stream.Collectors.toMap(Student::getId, s -> s));

        // Check for missing/invalid students
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

        // Batch save
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

        // Batch fetch all students
        List<Student> students = studentRepository.findAllByIds(ids);
        Map<Long, Student> studentMap = students.stream()
                .collect(java.util.stream.Collectors.toMap(Student::getId, s -> s));

        // Check for missing/invalid students
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

        // Batch save
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

        // Batch fetch all students
        List<Student> students = studentRepository.findAllByIds(ids);
        Map<Long, Student> studentMap = students.stream()
                .collect(java.util.stream.Collectors.toMap(Student::getId, s -> s));

        // Check for missing/invalid students
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
        toDelete.forEach(studentRepository::detach);

        // Delete users from auth-svc (FK CASCADE automatically deletes students)
        for (Student student : toDelete) {
            AuthServicePort.DeleteUserResult deleteResult = authServicePort.deleteUser(student.getUserId());
            if (!deleteResult.success()) {
                log.warn("Failed to delete user from auth-svc: {}", deleteResult.message());
            }
        }

        int successCount = toDelete.size();
        log.info("Batch delete completed: {} success, {} failures in tenant: {}", successCount, failures.size(), tenantSlug);
        return BatchOperationResult.partial(ids.size(), successCount, failures);
    }

    public BulkImportResult bulkImportStudents(Long academyId, MultipartFile file) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new AcademyNotFoundException(academyId));

        List<BulkImportResult.RowError> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRows++;

                try {
                    String userLoginId = getCellStringValue(row.getCell(0));
                    String name = getCellStringValue(row.getCell(1));
                    String phone = getCellStringValue(row.getCell(2));
                    String studentHighschool = getCellStringValue(row.getCell(3));
                    Integer studentBirthYear = getCellIntegerValue(row.getCell(4));
                    String studentMemo = getCellStringValue(row.getCell(5));

                    if (userLoginId == null || userLoginId.isBlank()) {
                        errors.add(new BulkImportResult.RowError(i + 1, "userLoginId", "User login ID is required"));
                        continue;
                    }

                    if (studentRepository.existsByUserLoginId(userLoginId)) {
                        errors.add(new BulkImportResult.RowError(i + 1, "userLoginId", "User login ID already exists"));
                        continue;
                    }

                    checkStudentQuota(tenantSlug);

                    // For bulk import, we need to create users in auth-svc first
                    // This is a simplified version that assumes users already exist
                    // TODO: Implement proper bulk user creation via auth-svc gRPC
                    errors.add(new BulkImportResult.RowError(i + 1, "userLoginId",
                        "Bulk import requires users to be pre-registered in auth service"));

                } catch (QuotaExceededException e) {
                    errors.add(new BulkImportResult.RowError(i + 1, "quota", "Student quota exceeded"));
                    break;
                } catch (Exception e) {
                    errors.add(new BulkImportResult.RowError(i + 1, "unknown", e.getMessage()));
                }
            }
        } catch (IOException e) {
            log.error("Failed to read Excel file", e);
            throw new RuntimeException("Failed to read Excel file", e);
        }

        log.info("Bulk import completed: {} success, {} failures in tenant: {}",
                 successCount, errors.size(), tenantSlug);

        return BulkImportResult.partial(totalRows, successCount, errors);
    }

    private void checkStudentQuota(String tenantSlug) {
        var result = billingServicePort.checkQuota(tenantSlug, "MAX_STUDENTS");
        if (!result.allowed()) {
            throw new QuotaExceededException("MAX_STUDENTS", result.currentUsage(), result.limit());
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private Integer getCellIntegerValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
}
