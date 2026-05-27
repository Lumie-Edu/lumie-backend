package com.lumie.tuition.adapter.out.persistence;

import com.lumie.tuition.domain.entity.StudentGuardian;
import com.lumie.tuition.domain.entity.StudentGuardianId;
import com.lumie.tuition.domain.repository.StudentGuardianRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGuardianJpaRepository
        extends StudentGuardianRepository, JpaRepository<StudentGuardian, StudentGuardianId> {
}
