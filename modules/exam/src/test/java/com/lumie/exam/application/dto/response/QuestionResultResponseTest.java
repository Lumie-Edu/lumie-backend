package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionResultResponseTest {

    @Test
    void incorrectAnswerKeepsPossibleScoreSeparateFromEarnedScore() {
        Exam exam = Exam.create("E2E", ExamCategory.GRADED, GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE, 1, Map.of("1", "1"), Map.of("1", 2),
                Map.of("1", "듣기"), null);
        QuestionResult result = QuestionResult.create(1, "2", "1", 2);

        QuestionResultResponse response = QuestionResultResponse.from(result, exam);

        assertThat(response.score()).isEqualTo(2);
        assertThat(response.earnedScore()).isZero();
        assertThat(response.isCorrect()).isFalse();
    }
}
