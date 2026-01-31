package com.lumie.exam.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_results",
       uniqueConstraints = @UniqueConstraint(columnNames = {"exam_result_id", "question_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_result_id", nullable = false)
    private ExamResult examResult;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "selected_choice", length = 10)
    private String selectedChoice;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Builder
    private QuestionResult(Integer questionNumber, String selectedChoice,
                          Boolean isCorrect, Integer score) {
        this.questionNumber = questionNumber;
        this.selectedChoice = selectedChoice;
        this.isCorrect = isCorrect;
        this.score = score;
    }

    public static QuestionResult create(Integer questionNumber, String selectedChoice,
                                        String correctAnswer, Integer maxScore) {
        boolean correct = selectedChoice != null && selectedChoice.equals(correctAnswer);
        return QuestionResult.builder()
                .questionNumber(questionNumber)
                .selectedChoice(selectedChoice)
                .isCorrect(correct)
                .score(correct ? maxScore : 0)
                .build();
    }

    void setExamResult(ExamResult examResult) {
        this.examResult = examResult;
    }

    public boolean isCorrect() {
        return Boolean.TRUE.equals(isCorrect);
    }

    public int getEarnedScore() {
        return isCorrect() ? score : 0;
    }
}
