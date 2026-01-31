package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Student;
import com.lumie.academy.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudentRepositoryAdapter implements StudentRepository {

    private final JpaStudentRepository jpaStudentRepository;

    @Override
    public Student save(Student student) {
        return jpaStudentRepository.save(student);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return jpaStudentRepository.findById(id);
    }

    @Override
    public Optional<Student> findByUserId(Long userId) {
        return jpaStudentRepository.findByUserId(userId);
    }

    @Override
    public Optional<Student> findByUserEmail(String email) {
        return jpaStudentRepository.findByUserEmail(email);
    }

    @Override
    public Page<Student> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaStudentRepository.findByAcademyId(academyId, pageable);
    }

    @Override
    public Page<Student> findByAcademyIdAndStatus(Long academyId, String status, Pageable pageable) {
        return jpaStudentRepository.findByAcademyIdAndStatus(academyId, status, pageable);
    }

    @Override
    public Page<Student> findAllByStatus(String status, Pageable pageable) {
        return jpaStudentRepository.findByStatus(status, pageable);
    }

    @Override
    public long countByAcademyId(Long academyId) {
        return jpaStudentRepository.countByAcademyId(academyId);
    }

    @Override
    public long countByStatus(String status) {
        return jpaStudentRepository.countByStatus(status);
    }

    @Override
    public boolean existsByUserEmail(String email) {
        return jpaStudentRepository.existsByUserEmail(email);
    }

    @Override
    public void delete(Student student) {
        jpaStudentRepository.delete(student);
    }
}
