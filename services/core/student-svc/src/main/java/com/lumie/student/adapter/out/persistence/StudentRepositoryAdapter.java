package com.lumie.student.adapter.out.persistence;

import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.lumie.student.adapter.out.persistence.StudentSpecification.*;

@Repository
@RequiredArgsConstructor
public class StudentRepositoryAdapter implements StudentRepository {

    private final JpaStudentRepository jpaStudentRepository;
    private final EntityManager entityManager;

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
    public List<Student> findAllByUserIds(List<Long> userIds) {
        return jpaStudentRepository.findByUserIdIn(userIds);
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
    public Page<Student> findAllByIsActive(Boolean isActive, Pageable pageable) {
        return jpaStudentRepository.findByIsActive(isActive, pageable);
    }

    @Override
    public Page<Student> findAll(Pageable pageable) {
        return jpaStudentRepository.findAll(pageable);
    }

    @Override
    public Page<Student> search(Boolean isActive, String search, Pageable pageable) {
        Specification<Student> spec = Specification
                .where(hasIsActive(isActive))
                .and(searchByKeyword(search));
        return jpaStudentRepository.findAll(spec, pageable);
    }

    @Override
    public long countByIsActive(Boolean isActive) {
        return jpaStudentRepository.countByIsActive(isActive);
    }

    @Override
    public long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to) {
        return jpaStudentRepository.countByCreatedAtBetween(from, to);
    }

    @Override
    public boolean existsByUserLoginId(String userLoginId) {
        return jpaStudentRepository.existsByUserLoginId(userLoginId);
    }

    @Override
    public boolean existsByPhoneInAnyField(String phone) {
        return jpaStudentRepository.existsByPhoneInAnyField(phone);
    }

    @Override
    public boolean existsByPhoneInAnyFieldExcluding(String phone, Long excludeId) {
        return jpaStudentRepository.existsByPhoneInAnyFieldExcluding(phone, excludeId);
    }

    @Override
    public void detach(Student student) {
        entityManager.detach(student);
    }
}
