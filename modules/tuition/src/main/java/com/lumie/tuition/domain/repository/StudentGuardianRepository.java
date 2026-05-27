package com.lumie.tuition.domain.repository;

import com.lumie.tuition.domain.entity.StudentGuardian;
import com.lumie.tuition.domain.entity.StudentGuardianId;

import java.util.List;
import java.util.Optional;

public interface StudentGuardianRepository {

    StudentGuardian save(StudentGuardian studentGuardian);

    Optional<StudentGuardian> findById(StudentGuardianId id);

    List<StudentGuardian> findByStudentId(Long studentId);

    List<StudentGuardian> findByGuardianId(Long guardianId);

    boolean existsByStudentIdAndGuardianId(Long studentId, Long guardianId);

    void deleteByStudentIdAndGuardianId(Long studentId, Long guardianId);
}
