package com.lumie.student.domain.entity;

import com.lumie.student.application.dto.response.StudentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StudentCanonicalIdentityTest {
    @Test
    void userIdentityIsTheOnlySourceForStudentResponses() {
        Student student = Student.create(7L, "student_7", "옛 이름", "01011112222",
                null, null, null, null);
        ReflectionTestUtils.setField(student, "canonicalName", "새 이름");
        ReflectionTestUtils.setField(student, "canonicalPhone", null);

        StudentResponse response = StudentResponse.from(student);
        assertThat(response.name()).isEqualTo("새 이름");
        assertThat(response.phone()).isNull();

        student.updateInfo("관리자 수정", "010-3333-4444", null, null, null, null);
        assertThat(student.getStudentName()).isEqualTo("관리자 수정");
        assertThat(student.getStudentPhone()).isEqualTo("01033334444");
    }
}
