package com.lumie.lecture.application.dto.response;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LectureResponse 테스트")
class LectureResponseTest {

    @Test
    @DisplayName("teacherId가 null인 강의 응답을 생성할 수 있다")
    void from_nullableTeacherId() {
        Lecture lecture = Lecture.create(1L, 10L, "강의", null, "https://example.com", LectureVisibility.TEACHER_ONLY, null);
        ReflectionTestUtils.setField(lecture, "teacherId", null);

        LectureResponse response = LectureResponse.from(lecture);

        assertThat(response.teacherId()).isNull();
    }
}
