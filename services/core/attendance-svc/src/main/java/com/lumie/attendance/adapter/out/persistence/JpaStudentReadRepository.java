package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.StudentReadModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaStudentReadRepository extends JpaRepository<StudentReadModel, Long> {

    Optional<StudentReadModel> findByUserId(Long userId);

    List<StudentReadModel> findByAcademyIdAndIsActiveTrue(Long academyId);
}
