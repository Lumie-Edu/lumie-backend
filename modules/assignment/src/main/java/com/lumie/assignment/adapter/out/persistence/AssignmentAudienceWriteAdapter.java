package com.lumie.assignment.adapter.out.persistence;

import com.lumie.assignment.application.port.out.AssignmentAudienceWritePort;
import com.lumie.assignment.domain.entity.AssignmentClassTarget;
import com.lumie.assignment.domain.entity.AssignmentStudentTarget;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentAudienceWriteAdapter implements AssignmentAudienceWritePort {

    private final AssignmentClassTargetJpaRepository classTargets;
    private final AssignmentStudentTargetJpaRepository studentTargets;

    @Override
    public void createTargets(Long assignmentId, List<Long> classIds, List<Long> studentIds) {
        classTargets.saveAllAndFlush(classIds.stream()
                .map(classId -> AssignmentClassTarget.create(assignmentId, classId))
                .toList());
        studentTargets.saveAllAndFlush(studentIds.stream()
                .map(studentId -> AssignmentStudentTarget.create(assignmentId, studentId))
                .toList());
    }
}
