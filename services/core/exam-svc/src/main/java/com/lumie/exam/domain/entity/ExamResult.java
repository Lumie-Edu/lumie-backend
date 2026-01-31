package com.lumie.exam.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_results",
       uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "grade")
    private Integer grade;

    @OneToMany(mappedBy = "examResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionResult> questionResults = new ArrayList<>();

    @Builder
    private ExamResult(Exam exam, Long studentId, Integer totalScore, Integer grade) {
        this.exam = exam;
        this.studentId = studentId;
        this.totalScore = totalScore;
        this.grade = grade;
    }

    public static ExamResult create(Exam exam, Long studentId, Integer totalScore) {
        return ExamResult.builder()
                .exam(exam)
                .studentId(studentId)
                .totalScore(totalScore)
                .build();
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public void addQuestionResult(QuestionResult questionResult) {
        this.questionResults.add(questionResult);
        questionResult.setExamResult(this);
    }

    public void updateScore(Integer newTotalScore) {
        this.totalScore = newTotalScore;
    }

    public boolean isPassed() {
        if (!exam.isPassFail() || exam.getPassScore() == null) {
            return false;
        }
        return totalScore >= exam.getPassScore();
    }

    public int getCorrectCount() {
        return (int) questionResults.stream()
                .filter(QuestionResult::isCorrect)
                .count();
    }

    public int getIncorrectCount() {
        return (int) questionResults.stream()
                .filter(qr -> !qr.isCorrect())
                .count();
    }
}
