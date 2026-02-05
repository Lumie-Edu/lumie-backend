package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Student;
import com.lumie.academy.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.lumie.academy.adapter.out.persistence.StudentSpecification.*;

@Repository
@RequiredArgsConstructor
public class StudentRepositoryAdapter implements StudentRepository {

    private final JpaStudentRepository jpaStudentRepository;

    @Override
    public Student save(Student student) {
        return jpaStudentRepository.save(student);
    }

    @Override
    public List<Student> saveAll(List<Student> students) {
        return jpaStudentRepository.saveAll(students);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return jpaStudentRepository.findById(id);
    }

    @Override
    public List<Student> findAllByIds(List<Long> ids) {
        return jpaStudentRepository.findAllById(ids);
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
    public Page<Student> search(Long academyId, Boolean isActive, String search, String searchField, Pageable pageable) {
        Specification<Student> spec = Specification
                .where(hasAcademyId(academyId))
                .and(hasIsActive(isActive))
                .and(searchByField(search, searchField));
        return jpaStudentRepository.findAll(spec, pageable);
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

    @Override
    public void deleteAll(List<Student> students) {
        jpaStudentRepository.deleteAllInBatch(students);
    }
}
