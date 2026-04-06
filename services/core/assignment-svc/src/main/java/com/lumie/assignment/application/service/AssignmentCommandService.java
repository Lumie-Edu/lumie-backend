package com.lumie.assignment.application.service;

import com.lumie.assignment.application.dto.AssignmentResponse;
import com.lumie.assignment.application.dto.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.CreateSubmissionRequest;
import com.lumie.assignment.application.dto.SubmissionResponse;
import com.lumie.assignment.application.dto.UpdateAssignmentRequest;
import com.lumie.assignment.application.dto.UpdateSubmissionRequest;
import com.lumie.assignment.application.port.out.AcademyServicePort;
import com.lumie.assignment.application.port.out.AdminServicePort;
import com.lumie.assignment.application.port.out.ClassServicePort;
import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.assignment.domain.exception.AssignmentNotFoundException;
import com.lumie.assignment.domain.exception.DuplicateSubmissionException;
import com.lumie.assignment.domain.exception.SubmissionNotFoundException;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentCommandService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final ClassServicePort classServicePort;
    private final AcademyServicePort academyServicePort;
    private final AdminServicePort adminServicePort;

    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        academyServicePort.getAcademy(request.academyId())
            .filter(AcademyServicePort.AcademyInfo::isActive)
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ACADEMY_NOT_FOUND,
                "Academy not found or inactive: " + request.academyId()));

        classServicePort.getClass(request.classId())
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: " + request.classId()));

        adminServicePort.getAdmin(request.createdBy())
            .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CREATOR_NOT_FOUND,
                "Creator not found with id: " + request.createdBy()));

        AssignmentType type = parseAssignmentType(request.type());

        Assignment entity = Assignment.create(
            request.classId(),
            request.academyId(),
            request.title(),
            request.description(),
            type,
            request.dueDate(),
            request.maxScore(),
            request.attachments(),
            request.createdBy()
        );

        Assignment saved = assignmentRepository.save(entity);

        log.info("Assignment created: {} in class: {}", saved.getId(), saved.getClassId());
        return AssignmentResponse.from(saved);
    }

    public AssignmentResponse updateAssignment(Long id, UpdateAssignmentRequest request) {
        Assignment entity = assignmentRepository.findById(id)
            .orElseThrow(() -> new AssignmentNotFoundException(id));

        String title = request.title() != null ? request.title() : entity.getTitle();
        String description = request.description() != null ? request.description() : entity.getDescription();
        AssignmentType type = request.type() != null ? parseAssignmentType(request.type()) : entity.getType();
        var dueDate = request.dueDate() != null ? request.dueDate() : entity.getDueDate();
        Integer maxScore = request.maxScore() != null ? request.maxScore() : entity.getMaxScore();
        var attachments = request.attachments() != null ? request.attachments() : entity.getAttachments();

        entity.updateInfo(title, description, type, dueDate, maxScore, attachments);

        if (request.status() != null) {
            AssignmentStatus newStatus = parseAssignmentStatus(request.status());
            switch (newStatus) {
                case ACTIVE -> entity.activate();
                case CLOSED -> entity.close();
                case DRAFT -> {} // no-op
            }
        }

        Assignment saved = assignmentRepository.save(entity);

        log.info("Assignment updated: {}", saved.getId());
        return AssignmentResponse.from(saved);
    }

    public void deleteAssignment(Long id) {
        Assignment entity = assignmentRepository.findById(id)
            .orElseThrow(() -> new AssignmentNotFoundException(id));

        entity.close();
        assignmentRepository.save(entity);

        log.info("Assignment closed (soft-deleted): {}", id);
    }

    public SubmissionResponse createSubmission(Long assignmentId, CreateSubmissionRequest request) {
        assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));

        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, request.studentId())) {
            throw new DuplicateSubmissionException(assignmentId, request.studentId());
        }

        AssignmentSubmission submission = AssignmentSubmission.create(assignmentId, request.studentId());
        submission.submit(request.content(), request.attachments());

        AssignmentSubmission saved = submissionRepository.save(submission);

        log.info("Submission created: {} for assignment: {}", saved.getId(), assignmentId);
        return SubmissionResponse.from(saved);
    }

    public SubmissionResponse updateSubmission(Long assignmentId, Long submissionId,
                                               UpdateSubmissionRequest request) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new SubmissionNotFoundException(submissionId));

        if (!submission.getAssignmentId().equals(assignmentId)) {
            throw new SubmissionNotFoundException(submissionId);
        }

        if (request.status() != null) {
            SubmissionStatus newStatus = parseSubmissionStatus(request.status());
            switch (newStatus) {
                case GRADED -> submission.grade(request.score(), request.feedback());
                case RETURNED -> submission.returnToStudent(request.feedback());
                case SUBMITTED, PENDING -> {} // no-op
            }
        } else if (request.score() != null || request.feedback() != null) {
            submission.grade(request.score(), request.feedback());
        }

        AssignmentSubmission saved = submissionRepository.save(submission);

        log.info("Submission {} updated for assignment: {}", submissionId, assignmentId);
        return SubmissionResponse.from(saved);
    }

    private AssignmentType parseAssignmentType(String type) {
        if (type == null) {
            return AssignmentType.HOMEWORK;
        }
        try {
            return AssignmentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS, "Invalid assignment type: " + type);
        }
    }

    private AssignmentStatus parseAssignmentStatus(String status) {
        try {
            return AssignmentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS, "Invalid assignment status: " + status);
        }
    }

    private SubmissionStatus parseSubmissionStatus(String status) {
        try {
            return SubmissionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AssignmentErrorCode.INVALID_STATUS, "Invalid submission status: " + status);
        }
    }
}
