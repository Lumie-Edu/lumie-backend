package com.lumie.exam.adapter.out.persistence;

public interface ResultWithAcademyProjection {
    Long getAcademyId();
    String getAcademyName();
    Long getStudentId();
    Integer getTotalScore();
    Integer getGrade();
}
