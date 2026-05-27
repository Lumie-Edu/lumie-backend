package com.lumie.tuition.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class StudentGuardianId implements Serializable {

    private Long studentId;
    private Long guardianId;

    public StudentGuardianId() {}

    public StudentGuardianId(Long studentId, Long guardianId) {
        this.studentId = studentId;
        this.guardianId = guardianId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentGuardianId other)) return false;
        return Objects.equals(studentId, other.studentId)
                && Objects.equals(guardianId, other.guardianId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, guardianId);
    }
}
