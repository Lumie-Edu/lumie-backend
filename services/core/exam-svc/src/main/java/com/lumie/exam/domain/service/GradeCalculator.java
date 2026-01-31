package com.lumie.exam.domain.service;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.vo.ExamCategory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class GradeCalculator {

    private static final int[] GRADE_PERCENTILE_THRESHOLDS = {4, 11, 23, 40, 60, 77, 89, 96, 100};

    public void calculateAndAssignGrades(Exam exam, List<ExamResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        if (exam.getCategory() == ExamCategory.GRADED) {
            calculateRelativeGrades(results);
        } else if (exam.getCategory() == ExamCategory.PASS_FAIL) {
            calculatePassFailGrades(exam, results);
        }
    }

    public int calculateGradeForScore(int score, List<Integer> allScores) {
        if (allScores == null || allScores.isEmpty()) {
            return 5;
        }

        long higherCount = allScores.stream()
                .filter(s -> s > score)
                .count();

        double percentile = (double) higherCount / allScores.size() * 100;

        return getGradeFromPercentile(percentile);
    }

    public int calculatePassFailGrade(int score, Integer passScore) {
        if (passScore == null) {
            return 0;
        }
        return score >= passScore ? 0 : -1;
    }

    private void calculateRelativeGrades(List<ExamResult> results) {
        List<ExamResult> sortedResults = results.stream()
                .sorted(Comparator.comparingInt(ExamResult::getTotalScore).reversed())
                .toList();

        int totalCount = sortedResults.size();

        for (int i = 0; i < sortedResults.size(); i++) {
            ExamResult result = sortedResults.get(i);
            double percentile = (double) i / totalCount * 100;
            int grade = getGradeFromPercentile(percentile);
            result.setGrade(grade);
        }
    }

    private void calculatePassFailGrades(Exam exam, List<ExamResult> results) {
        Integer passScore = exam.getPassScore();
        if (passScore == null) {
            results.forEach(r -> r.setGrade(0));
            return;
        }

        for (ExamResult result : results) {
            int grade = result.getTotalScore() >= passScore ? 0 : -1;
            result.setGrade(grade);
        }
    }

    private int getGradeFromPercentile(double percentile) {
        for (int i = 0; i < GRADE_PERCENTILE_THRESHOLDS.length; i++) {
            if (percentile < GRADE_PERCENTILE_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 9;
    }
}
