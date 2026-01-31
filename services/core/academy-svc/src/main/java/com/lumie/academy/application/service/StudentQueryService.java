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

    public StudentResponse getStudentByEmail(String email) {
        Student student = studentRepository.findByUserEmail(email)
                .orElseThrow(() -> new StudentNotFoundException(email));
        return StudentResponse.from(student);
    }

    public Page<StudentResponse> getStudentsByAcademy(Long academyId, Pageable pageable) {
        return studentRepository.findByAcademyId(academyId, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getActiveStudentsByAcademy(Long academyId, Pageable pageable) {
        return studentRepository.findByAcademyIdAndStatus(academyId, "ACTIVE", pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getAllActiveStudents(Pageable pageable) {
        return studentRepository.findAllByStatus("ACTIVE", pageable)
                .map(StudentResponse::from);
    }

    public long countStudentsByAcademy(Long academyId) {
        return studentRepository.countByAcademyId(academyId);
    }

    public long countActiveStudents() {
        return studentRepository.countByStatus("ACTIVE");
    }

    public boolean existsByEmail(String email) {
        return studentRepository.existsByUserEmail(email);
    }
}
