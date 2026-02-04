package com.lumie.exam.domain.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticsCalculator {

    /**
     * 평균 계산
     */
    public double calculateMean(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 표준편차 계산 (모집단 표준편차)
     */
    public double calculateStandardDeviation(List<Integer> scores) {
        if (scores == null || scores.size() < 2) {
            return 0.0;
        }

        double mean = calculateMean(scores);
        double sumSquaredDiff = scores.stream()
                .mapToDouble(score -> Math.pow(score - mean, 2))
                .sum();

        return Math.sqrt(sumSquaredDiff / scores.size());
    }

    /**
     * 백분위 계산 (해당 점수보다 낮은 점수의 비율)
     */
    public double calculatePercentile(int score, List<Integer> allScores) {
        if (allScores == null || allScores.isEmpty()) {
            return 0.0;
        }

        long lowerCount = allScores.stream()
                .filter(s -> s < score)
                .count();

        return (double) lowerCount / allScores.size() * 100;
    }

    /**
     * 석차 계산 (1부터 시작)
     */
    public int calculateRank(int score, List<Integer> allScores) {
        if (allScores == null || allScores.isEmpty()) {
            return 1;
        }

        long higherCount = allScores.stream()
                .filter(s -> s > score)
                .count();

        return (int) higherCount + 1;
    }

    /**
     * 변동계수 (CV = 표준편차/평균 * 100)
     * 성적 안정성 지표로 사용 - 낮을수록 안정적
     */
    public double calculateCoefficientOfVariation(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }

        double mean = calculateMean(scores);
        if (mean == 0) {
            return 0.0;
        }

        double stdDev = calculateStandardDeviation(scores);
        return (stdDev / mean) * 100;
    }

    /**
     * Z-score 정규화 (시험 난이도 보정)
     * Z = (점수 - 평균) / 표준편차
     */
    public double calculateZScore(int score, double mean, double stdDev) {
        if (stdDev == 0) {
            return 0.0;
        }
        return (score - mean) / stdDev;
    }

    /**
     * Z-score를 기반으로 정규화 점수 계산 (평균 50, 표준편차 10)
     */
    public double calculateNormalizedScore(double zScore) {
        return 50 + (zScore * 10);
    }

    /**
     * 안정성 레벨 판정
     * CV < 5%: VERY_STABLE (매우 안정)
     * CV < 10%: STABLE (안정)
     * CV < 15%: MODERATE (보통)
     * CV < 20%: UNSTABLE (불안정)
     * CV >= 20%: VERY_UNSTABLE (매우 불안정)
     */
    public String determineStabilityLevel(double cv) {
        if (cv < 5) {
            return "VERY_STABLE";
        } else if (cv < 10) {
            return "STABLE";
        } else if (cv < 15) {
            return "MODERATE";
        } else if (cv < 20) {
            return "UNSTABLE";
        } else {
            return "VERY_UNSTABLE";
        }
    }

    /**
     * 성장률 계산 (%)
     */
    public double calculateGrowthRate(double previousScore, double currentScore) {
        if (previousScore == 0) {
            return currentScore > 0 ? 100.0 : 0.0;
        }
        return ((currentScore - previousScore) / previousScore) * 100;
    }

    /**
     * 선지 선택률 계산 (%)
     */
    public double calculateChoiceRate(int choiceCount, int totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        return ((double) choiceCount / totalCount) * 100;
    }
}
