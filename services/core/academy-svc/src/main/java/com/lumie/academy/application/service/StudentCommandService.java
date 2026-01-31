package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.BulkImportResult;
import com.lumie.academy.application.dto.StudentRequest;
import com.lumie.academy.application.dto.StudentResponse;
import com.lumie.academy.application.dto.StudentUpdateRequest;
import com.lumie.academy.application.port.out.BillingServicePort;
import com.lumie.academy.application.port.out.MemberEventPublisherPort;
import com.lumie.academy.domain.entity.Academy;
import com.lumie.academy.domain.entity.Student;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.exception.DuplicateEmailException;
import com.lumie.academy.domain.exception.QuotaExceededException;
import com.lumie.academy.domain.exception.StudentNotFoundException;
import com.lumie.academy.domain.repository.AcademyRepository;
import com.lumie.academy.domain.repository.StudentRepository;
import com.lumie.academy.infrastructure.tenant.TenantContextHolder;
import com.lumie.messaging.event.MemberCreatedEvent;
import com.lumie.messaging.event.MemberDeletedEvent;
import com.lumie.messaging.event.MemberUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentCommandService {

    private final StudentRepository studentRepository;
    private final AcademyRepository academyRepository;
    private final PasswordEncoder passwordEncoder;
    private final BillingServicePort billingServicePort;
    private final MemberEventPublisherPort eventPublisher;

    public StudentResponse registerStudent(StudentRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        checkStudentQuota(tenantSlug);

        if (studentRepository.existsByUserEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Academy academy = academyRepository.findById(request.academyId())
                .orElseThrow(() -> new AcademyNotFoundException(request.academyId()));

        String passwordHash = passwordEncoder.encode(request.password());

        Student student = Student.create(
            request.email(),
            passwordHash,
            request.name(),
            request.phone(),
            academy,
            request.studentNumber(),
            request.grade(),
            request.schoolName(),
            request.parentName(),
            request.parentPhone()
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

        log.info("Student registered: {} in tenant: {}", saved.getStudentEmail(), tenantSlug);
        return StudentResponse.from(saved);
    }

    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        student.updateInfo(
            request.name(),
            request.phone(),
            request.grade(),
            request.schoolName(),
            request.parentName(),
            request.parentPhone()
        );

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
                    String email = getCellStringValue(row.getCell(0));
                    String password = getCellStringValue(row.getCell(1));
                    String name = getCellStringValue(row.getCell(2));
                    String phone = getCellStringValue(row.getCell(3));
                    String studentNumber = getCellStringValue(row.getCell(4));
                    String grade = getCellStringValue(row.getCell(5));
                    String schoolName = getCellStringValue(row.getCell(6));
                    String parentName = getCellStringValue(row.getCell(7));
                    String parentPhone = getCellStringValue(row.getCell(8));

                    if (email == null || email.isBlank()) {
                        errors.add(new BulkImportResult.RowError(i + 1, "email", "Email is required"));
                        continue;
                    }

                    if (studentRepository.existsByUserEmail(email)) {
                        errors.add(new BulkImportResult.RowError(i + 1, "email", "Email already exists"));
                        continue;
                    }

                    checkStudentQuota(tenantSlug);

                    String passwordHash = passwordEncoder.encode(password != null ? password : "default123");

                    Student student = Student.create(
                        email, passwordHash, name, phone, academy,
                        studentNumber, grade, schoolName, parentName, parentPhone
                    );

                    Student saved = studentRepository.save(student);

                    eventPublisher.publish(new MemberCreatedEvent(
                        saved.getId(), tenantSlug, "STUDENT",
                        saved.getStudentName(), saved.getStudentEmail(), academy.getId()
                    ));

                    successCount++;
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
}
