package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.ItemAnalysisResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.service.StatisticsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Item analysis: per-question p-value (difficulty) and discrimination index
 * computed from top/bottom Kelley sample partitions, then plotted on the
 * difficulty × discrimination quadrant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemAnalysisQueryService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;

    public ItemAnalysisResponse getItemAnalysis(Long examId) {
        log.debug("Getting item analysis for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        int participantCount = results.size();

        if (participantCount < 2) {
            return new ItemAnalysisResponse(
                    examId, exam.getName(), participantCount, 0, true, Collections.emptyList()
            );
        }

        List<ExamResult> sorted = results.stream()
                .sorted(Comparator.comparingInt(ExamResult::getTotalScore).reversed())
                .toList();

        int sampleSize = statisticsCalculator.kelleySampleSize(participantCount);
        List<ExamResult> top = sorted.subList(0, sampleSize);
        List<ExamResult> bottom = sorted.subList(participantCount - sampleSize, participantCount);
        boolean reliabilityWarning = participantCount < 10;

        List<ItemAnalysisResponse.ItemPoint> items = new ArrayList<>();
        for (int qNum = 1; qNum <= exam.getTotalQuestions(); qNum++) {
            int allCorrect = countCorrect(results, qNum);
            int topCorrect = countCorrect(top, qNum);
            int bottomCorrect = countCorrect(bottom, qNum);

            double pValue = (double) allCorrect / participantCount;
            double discrimination = statisticsCalculator.calculateDiscrimination(
                    topCorrect, top.size(), bottomCorrect, bottom.size()
            );
            ItemAnalysisResponse.Quadrant quadrant = classifyQuadrant(pValue, discrimination);

            items.add(new ItemAnalysisResponse.ItemPoint(
                    qNum,
                    exam.getQuestionType(qNum),
                    Math.round(pValue * 1000) / 1000.0,
                    Math.round(discrimination * 1000) / 1000.0,
                    quadrant
            ));
        }

        return new ItemAnalysisResponse(
                examId, exam.getName(), participantCount, sampleSize, reliabilityWarning, items
        );
    }

    private int countCorrect(List<ExamResult> results, int questionNumber) {
        int correct = 0;
        for (ExamResult result : results) {
            for (QuestionResult qr : result.getQuestionResults()) {
                if (qr.getQuestionNumber() == questionNumber && qr.isCorrect()) {
                    correct++;
                    break;
                }
            }
        }
        return correct;
    }

    private ItemAnalysisResponse.Quadrant classifyQuadrant(double pValue, double discrimination) {
        if (pValue > 0.8) return ItemAnalysisResponse.Quadrant.TOO_EASY;
        if (pValue < 0.3) return ItemAnalysisResponse.Quadrant.TOO_HARD;
        if (discrimination >= 0.3) return ItemAnalysisResponse.Quadrant.GOOD;
        return ItemAnalysisResponse.Quadrant.DISCARD;
    }
}
