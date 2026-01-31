package com.lumie.exam.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.exam.domain.vo.ExamCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "exams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ExamCategory category;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answers", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> correctAnswers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_scores", nullable = false, columnDefinition = "jsonb")
    private Map<String, Integer> questionScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_types", columnDefinition = "jsonb")
    private Map<String, String> questionTypes;

    @Column(name = "pass_score")
    private Integer passScore;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamResult> results = new ArrayList<>();

    @Builder
    private Exam(String name, ExamCategory category, Integer totalQuestions,
                 Map<String, String> correctAnswers, Map<String, Integer> questionScores,
                 Map<String, String> questionTypes, Integer passScore) {
        this.name = name;
        this.category = category;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.questionScores = questionScores;
        this.questionTypes = questionTypes;
        this.passScore = passScore;
    }

    public static Exam create(String name, ExamCategory category, Integer totalQuestions,
                              Map<String, String> correctAnswers, Map<String, Integer> questionScores,
                              Map<String, String> questionTypes, Integer passScore) {
        return Exam.builder()
                .name(name)
                .category(category != null ? category : ExamCategory.GRADED)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .questionScores(questionScores)
                .questionTypes(questionTypes)
                .passScore(passScore)
                .build();
    }

    public void update(String name, ExamCategory category, Integer totalQuestions,
                       Map<String, String> correctAnswers, Map<String, Integer> questionScores,
                       Map<String, String> questionTypes, Integer passScore) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (totalQuestions != null) {
            this.totalQuestions = totalQuestions;
        }
        if (correctAnswers != null) {
            this.correctAnswers = correctAnswers;
        }
        if (questionScores != null) {
            this.questionScores = questionScores;
        }
        if (questionTypes != null) {
            this.questionTypes = questionTypes;
        }
        if (passScore != null) {
            this.passScore = passScore;
        }
    }

    public int calculateTotalPossibleScore() {
        return questionScores.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getScoreForQuestion(int questionNumber) {
        return questionScores.getOrDefault(String.valueOf(questionNumber), 0);
    }

    public String getCorrectAnswerForQuestion(int questionNumber) {
        return correctAnswers.get(String.valueOf(questionNumber));
    }

    public String getQuestionType(int questionNumber) {
        return questionTypes != null ? questionTypes.get(String.valueOf(questionNumber)) : null;
    }

    public boolean isGraded() {
        return category == ExamCategory.GRADED;
    }

    public boolean isPassFail() {
        return category == ExamCategory.PASS_FAIL;
    }
}
