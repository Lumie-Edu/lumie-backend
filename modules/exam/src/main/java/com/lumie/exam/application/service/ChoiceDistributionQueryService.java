package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.ChoiceDistributionResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-question choice distribution: how many people picked each option,
 * which option was correct, and the most "attractive" wrong answers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChoiceDistributionQueryService {

    private static final String NO_ANSWER = "NO_ANSWER";

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;

    public ChoiceDistributionResponse getChoiceDistribution(Long examId) {
        log.debug("Getting choice distribution for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        int totalParticipants = results.size();

        Map<Integer, Map<String, Integer>> questionChoiceCounts = new HashMap<>();

        for (ExamResult result : results) {
            for (QuestionResult qr : result.getQuestionResults()) {
                int qNum = qr.getQuestionNumber();
                String choice = qr.getSelectedChoice();
                if (choice == null) choice = NO_ANSWER;

                questionChoiceCounts.computeIfAbsent(qNum, k -> new HashMap<>());
                questionChoiceCounts.get(qNum).merge(choice, 1, Integer::sum);
            }
        }

        List<ChoiceDistributionResponse.QuestionChoiceDistribution> questions = new ArrayList<>();

        for (int qNum = 1; qNum <= exam.getTotalQuestions(); qNum++) {
            String correctAnswer = exam.getCorrectAnswerForQuestion(qNum);
            String questionType = exam.getQuestionType(qNum);
            Map<String, Integer> choiceCounts = questionChoiceCounts.getOrDefault(qNum, new HashMap<>());

            int correctCount = choiceCounts.getOrDefault(correctAnswer, 0);
            double correctRate = statisticsCalculator.calculateChoiceRate(correctCount, totalParticipants);

            Map<String, ChoiceDistributionResponse.ChoiceStats> choiceDistribution = new LinkedHashMap<>();
            for (String choice : List.of("1", "2", "3", "4", "5")) {
                int count = choiceCounts.getOrDefault(choice, 0);
                double percentage = statisticsCalculator.calculateChoiceRate(count, totalParticipants);
                choiceDistribution.put(choice, new ChoiceDistributionResponse.ChoiceStats(
                        count, Math.round(percentage * 10) / 10.0, choice.equals(correctAnswer)
                ));
            }

            List<ChoiceDistributionResponse.AttractiveDistractor> attractiveDistractors = choiceCounts.entrySet().stream()
                    .filter(e -> !e.getKey().equals(correctAnswer) && !e.getKey().equals(NO_ANSWER))
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(2)
                    .map(e -> new ChoiceDistributionResponse.AttractiveDistractor(
                            e.getKey(), e.getValue(),
                            Math.round(statisticsCalculator.calculateChoiceRate(e.getValue(), totalParticipants) * 10) / 10.0
                    ))
                    .toList();

            questions.add(new ChoiceDistributionResponse.QuestionChoiceDistribution(
                    qNum, questionType, correctAnswer, correctCount,
                    Math.round(correctRate * 10) / 10.0, choiceDistribution, attractiveDistractors
            ));
        }

        return new ChoiceDistributionResponse(examId, exam.getName(), totalParticipants, questions);
    }
}
