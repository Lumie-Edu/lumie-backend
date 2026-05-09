package com.lumie.exam.domain.service;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class GradeCalculator {

    // 9등급제 상대평가 백분위 기준 (기존 수능)
    private static final int[] NINE_GRADE_PERCENTILE_THRESHOLDS = {4, 11, 23, 40, 60, 77, 89, 96, 100};

    // 5등급제 상대평가 백분위 기준 (2028학년도 수능부터)
    private static final int[] FIVE_GRADE_PERCENTILE_THRESHOLDS = {10, 35, 65, 90, 100};

    // 9등급제 절대평가 점수 기준
    private static final int[] NINE_GRADE_ABSOLUTE_SCORE_THRESHOLDS = {90, 80, 70, 60, 50, 40, 30, 20, 0};

    // 5등급제 절대평가 점수 기준
    private static final int[] FIVE_GRADE_ABSOLUTE_SCORE_THRESHOLDS = {90, 80, 60, 40, 0};

    /**
     * 절대평가일 때만 등급 계산하여 저장 (저장용)
     * 상대평가는 조회 시 계산하므로 저장하지 않음
     */
    public void calculateAndAssignGradeIfAbsolute(Exam exam, ExamResult result) {
        if (exam.getCategory() == ExamCategory.PASS_FAIL) {
            // 합불제
            Integer passScore = exam.getPassScore();
            if (passScore != null) {
                result.setGrade(result.getTotalScore() >= passScore ? 0 : -1);
            }
        } else if (exam.getCategory() == ExamCategory.GRADED &&
                   exam.getGradingType() == GradingType.ABSOLUTE) {
            // 절대평가
            GradeScale scale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;
            result.setGrade(getGradeFromScore(result.getTotalScore(), scale));
        }
        // 상대평가는 grade를 null로 유지 (조회 시 계산)
    }

    /**
     * 상대평가 등급 계산 (조회용 - 저장하지 않음)
     */
    public int calculateRelativeGrade(int score, List<Integer> allScores) {
        return calculateRelativeGrade(score, allScores, GradeScale.NINE_GRADE);
    }

    /**
     * 상대평가 등급 계산 (등급 체계 지정)
     */
    public int calculateRelativeGrade(int score, List<Integer> allScores, GradeScale gradeScale) {
        if (allScores == null || allScores.isEmpty()) {
            return gradeScale == GradeScale.FIVE_GRADE ? 3 : 5;
        }

        long higherCount = allScores.stream()
                .filter(s -> s > score)
                .count();

        double percentile = (double) higherCount / allScores.size() * 100;
        return getGradeFromPercentile(percentile, gradeScale);
    }

    /**
     * 전체 결과에 등급 일괄 계산 (배치용)
     */
    public void calculateAndAssignGrades(Exam exam, List<ExamResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        if (exam.getCategory() == ExamCategory.GRADED) {
            if (exam.getGradingType() == GradingType.RELATIVE) {
                calculateRelativeGrades(results, gradeScale);
            } else {
                calculateAbsoluteGrades(results, gradeScale);
            }
        } else if (exam.getCategory() == ExamCategory.PASS_FAIL) {
            calculatePassFailGrades(exam, results);
        }
    }

    /**
     * 상대평가 - 백분위 기반 등급 계산
     */
    private void calculateRelativeGrades(List<ExamResult> results, GradeScale gradeScale) {
        List<ExamResult> sortedResults = results.stream()
                .sorted(Comparator.comparingInt(ExamResult::getTotalScore).reversed())
                .toList();

        int totalCount = sortedResults.size();

        for (int i = 0; i < sortedResults.size(); i++) {
            ExamResult result = sortedResults.get(i);
            double percentile = (double) i / totalCount * 100;
            int grade = getGradeFromPercentile(percentile, gradeScale);
            result.setGrade(grade);
        }
    }

    /**
     * 절대평가 - 점수 기반 등급 계산
     */
    private void calculateAbsoluteGrades(List<ExamResult> results, GradeScale gradeScale) {
        for (ExamResult result : results) {
            int grade = getGradeFromScore(result.getTotalScore(), gradeScale);
            result.setGrade(grade);
        }
    }

    /**
     * 합불제 - 합격/불합격 계산
     */
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

    /**
     * 상대평가용 - 백분위로 등급 계산
     */
    private int getGradeFromPercentile(double percentile, GradeScale gradeScale) {
        int[] thresholds = gradeScale == GradeScale.FIVE_GRADE
                ? FIVE_GRADE_PERCENTILE_THRESHOLDS
                : NINE_GRADE_PERCENTILE_THRESHOLDS;

        for (int i = 0; i < thresholds.length; i++) {
            if (percentile < thresholds[i]) {
                return i + 1;
            }
        }
        return gradeScale == GradeScale.FIVE_GRADE ? 5 : 9;
    }

    /**
     * 절대평가용 - 점수로 등급 계산
     */
    private int getGradeFromScore(int score, GradeScale gradeScale) {
        int[] thresholds = gradeScale == GradeScale.FIVE_GRADE
                ? FIVE_GRADE_ABSOLUTE_SCORE_THRESHOLDS
                : NINE_GRADE_ABSOLUTE_SCORE_THRESHOLDS;

        for (int i = 0; i < thresholds.length; i++) {
            if (score >= thresholds[i]) {
                return i + 1;
            }
        }
        return gradeScale == GradeScale.FIVE_GRADE ? 5 : 9;
    }

    // 통계 조회용 메서드들

    public int calculateGradeForScore(int score, List<Integer> allScores, GradingType gradingType) {
        return calculateGradeForScore(score, allScores, gradingType, GradeScale.NINE_GRADE);
    }

    public int calculateGradeForScore(int score, List<Integer> allScores, GradingType gradingType, GradeScale gradeScale) {
        if (gradingType == GradingType.ABSOLUTE) {
            return getGradeFromScore(score, gradeScale);
        }

        // RELATIVE
        if (allScores == null || allScores.isEmpty()) {
            return gradeScale == GradeScale.FIVE_GRADE ? 3 : 5;
        }

        long higherCount = allScores.stream()
                .filter(s -> s > score)
                .count();

        double percentile = (double) higherCount / allScores.size() * 100;
        return getGradeFromPercentile(percentile, gradeScale);
    }

    /**
     * 등급별 커트라인 점수 계산
     */
    public Integer calculateCutoffScore(int grade, List<Integer> allScores, GradingType gradingType) {
        return calculateCutoffScore(grade, allScores, gradingType, GradeScale.NINE_GRADE);
    }

    /**
     * 등급별 커트라인 점수 계산 (등급 체계 지정)
     * @param grade 등급 (1-5 또는 1-9)
     * @param allScores 전체 점수 목록 (상대평가용)
     * @param gradingType 평가 방식
     * @param gradeScale 등급 체계 (5등급제 또는 9등급제)
     * @return 해당 등급의 커트라인 점수
     */
    public Integer calculateCutoffScore(int grade, List<Integer> allScores, GradingType gradingType, GradeScale gradeScale) {
        int[] absoluteThresholds = gradeScale == GradeScale.FIVE_GRADE
                ? FIVE_GRADE_ABSOLUTE_SCORE_THRESHOLDS
                : NINE_GRADE_ABSOLUTE_SCORE_THRESHOLDS;
        int[] relativeThresholds = gradeScale == GradeScale.FIVE_GRADE
                ? FIVE_GRADE_PERCENTILE_THRESHOLDS
                : NINE_GRADE_PERCENTILE_THRESHOLDS;

        if (gradingType == GradingType.ABSOLUTE) {
            // 절대평가: 고정 커트라인
            if (grade >= 1 && grade <= absoluteThresholds.length) {
                return absoluteThresholds[grade - 1];
            }
            return 0;
        }

        // 상대평가: 백분위 기반 동적 커트라인
        if (allScores == null || allScores.isEmpty()) {
            return null;
        }

        List<Integer> sortedScores = allScores.stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (grade < 1 || grade > relativeThresholds.length) {
            return null;
        }

        int percentileThreshold = relativeThresholds[grade - 1];
        int cutoffIndex = (int) Math.ceil(sortedScores.size() * percentileThreshold / 100.0) - 1;
        cutoffIndex = Math.max(0, Math.min(cutoffIndex, sortedScores.size() - 1));

        return sortedScores.get(cutoffIndex);
    }

    /**
     * 등급 체계에 따른 최대 등급 반환
     */
    public int getMaxGrade(GradeScale gradeScale) {
        return gradeScale == GradeScale.FIVE_GRADE ? 5 : 9;
    }
}
