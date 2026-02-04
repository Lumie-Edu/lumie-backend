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
    public Optional<Student> findByUserLoginId(String userLoginId) {
        return jpaStudentRepository.findByUserLoginId(userLoginId);
    }

    @Override
    public Optional<Student> findByPhone(String phone) {
        return jpaStudentRepository.findByPhone(phone);
    }

    @Override
    public Page<Student> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaStudentRepository.findByAcademyId(academyId, pageable);
    }

    @Override
    public Page<Student> findByAcademyIdAndIsActive(Long academyId, Boolean isActive, Pageable pageable) {
        return jpaStudentRepository.findByAcademyIdAndIsActive(academyId, isActive, pageable);
    }

    @Override
    public Page<Student> findAllByIsActive(Boolean isActive, Pageable pageable) {
        return jpaStudentRepository.findByIsActive(isActive, pageable);
    }

    @Override
    public Page<Student> findAll(Pageable pageable) {
        return jpaStudentRepository.findAll(pageable);
    }

    @Override
    public long countByAcademyId(Long academyId) {
        return jpaStudentRepository.countByAcademyId(academyId);
    }

    @Override
    public long countByIsActive(Boolean isActive) {
        return jpaStudentRepository.countByIsActive(isActive);
    }

    @Override
    public boolean existsByUserLoginId(String userLoginId) {
        return jpaStudentRepository.existsByUserLoginId(userLoginId);
    }

    @Override
    public void delete(Student student) {
        jpaStudentRepository.delete(student);
    }
}
