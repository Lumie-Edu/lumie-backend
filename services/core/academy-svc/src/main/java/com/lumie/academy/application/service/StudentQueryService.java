package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.StudentResponse;
import com.lumie.academy.domain.entity.Student;
import com.lumie.academy.domain.exception.StudentNotFoundException;
import com.lumie.academy.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentQueryService {

    private final StudentRepository studentRepository;

    public StudentResponse getStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
        return StudentResponse.from(student);
    }

    public StudentResponse getStudentByUserLoginId(String userLoginId) {
        Student student = studentRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new StudentNotFoundException(userLoginId));
        return StudentResponse.from(student);
    }

    public StudentResponse getStudentByPhone(String phone) {
        Student student = studentRepository.findByPhone(phone)
                .orElseThrow(() -> new StudentNotFoundException("phone: " + phone));
        return StudentResponse.from(student);
    }

    public Page<StudentResponse> getStudentsByAcademy(Long academyId, Pageable pageable) {
        return studentRepository.findByAcademyId(academyId, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getActiveStudentsByAcademy(Long academyId, Pageable pageable) {
        return studentRepository.findByAcademyIdAndIsActive(academyId, true, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getAllActiveStudents(Pageable pageable) {
        return studentRepository.findAllByIsActive(true, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getAllStudents(Boolean isActive, Pageable pageable) {
        if (isActive == null) {
            return studentRepository.findAll(pageable)
                    .map(StudentResponse::from);
        }
        return studentRepository.findAllByIsActive(isActive, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getStudents(Long academyId, Boolean isActive, Pageable pageable) {
        if (academyId != null && isActive != null) {
            return studentRepository.findByAcademyIdAndIsActive(academyId, isActive, pageable)
                    .map(StudentResponse::from);
        } else if (academyId != null) {
            return studentRepository.findByAcademyId(academyId, pageable)
                    .map(StudentResponse::from);
        } else if (isActive != null) {
            return studentRepository.findAllByIsActive(isActive, pageable)
                    .map(StudentResponse::from);
        } else {
            return studentRepository.findAll(pageable)
                    .map(StudentResponse::from);
        }
    }

    public long countStudentsByAcademy(Long academyId) {
        return studentRepository.countByAcademyId(academyId);
    }

    public long countActiveStudents() {
        return studentRepository.countByIsActive(true);
    }

    public boolean existsByUserLoginId(String userLoginId) {
        return studentRepository.existsByUserLoginId(userLoginId);
    }
}
