package com.lumie.lecture.application.dto.response;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LectureResponse 테스트")
class LectureResponseTest {

    @Test
    @DisplayName("teacherId가 null인 강의 응답을 생성할 수 있다")
    void from_nullableTeacherId() {
        Lecture lecture = Lecture.create(10L, "강의", null, "https://example.com", LectureVisibility.TEACHER_ONLY, null);
        ReflectionTestUtils.setField(lecture, "teacherId", null);

        LectureResponse response = LectureResponse.from(lecture);

        assertThat(response.teacherId()).isNull();
    }

    @Test
    @DisplayName("응답의 classId와 classIds는 대상 수업 목록에서만 계산한다")
    void from_usesClassTargetsForCompatibilityFields() {
        Lecture lecture = Lecture.create(10L, "강의", null, null, LectureVisibility.STUDENT, null);

        LectureResponse targeted = LectureResponse.from(lecture, List.of(2L, 3L));
        LectureResponse targetless = LectureResponse.from(lecture, List.of());

        assertThat(targeted.classId()).isEqualTo(2L);
        assertThat(targeted.classIds()).containsExactly(2L, 3L);
        assertThat(targetless.classId()).isNull();
        assertThat(targetless.classIds()).isEmpty();
    }
}
