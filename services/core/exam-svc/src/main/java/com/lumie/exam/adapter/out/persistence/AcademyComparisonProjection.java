package com.lumie.exam.adapter.out.persistence;

public interface AcademyComparisonProjection {
    Long getAcademyId();
    String getAcademyName();
    Integer getParticipantCount();
    Double getAverage();
    Integer getGrade1Count();
}
