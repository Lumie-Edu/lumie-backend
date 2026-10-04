package com.lumie.assignment.application.port.out;

import java.util.List;

public interface AssignmentAudienceWritePort {

    void createTargets(Long assignmentId, List<Long> classIds, List<Long> studentIds);
}
