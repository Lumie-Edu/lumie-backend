package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.ExamAnswerRegradePreviewRequest;
import com.lumie.exam.application.dto.request.ExamAnswerRegradeConfirmRequest;
import com.lumie.exam.application.dto.response.ExamAnswerRegradeRevisionResponse;
import com.lumie.exam.application.dto.response.ExamAnswerRegradePreviewResponse;
import com.lumie.exam.application.dto.response.ExamAnswerRegradePreviewResponse.AnswerChange;
import com.lumie.exam.application.dto.response.ExamAnswerKeyRevisionResponse;
import com.lumie.exam.application.port.out.ExamAnswerKeyRevisionPersistencePort;
import com.lumie.exam.application.port.out.ExamPersistencePort;
import com.lumie.exam.application.port.out.OmrGradingJobPersistencePort;
import com.lumie.exam.application.port.out.ExamResultPersistencePort;
import com.lumie.exam.application.port.out.OmrSubmissionPersistencePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamAnswerKeyRevision;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.entity.OmrSubmissionQuestion;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.vo.ExamSource;
import com.lumie.exam.domain.vo.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.HexFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ExamRegradeService {

    private final ExamPersistencePort examRepository;
    private final OmrGradingJobPersistencePort omrGradingJobRepository;
    private final ExamResultPersistencePort resultRepository;
    private final OmrSubmissionPersistencePort submissionRepository;
    private final ExamAnswerKeyRevisionPersistencePort revisionRepository;
    private final GradeCalculator gradeCalculator;
    private final ExamRegradeCalculator calculator;

    @Transactional(readOnly = true)
    public ExamAnswerRegradePreviewResponse preview(
            Long examId, ExamAnswerRegradePreviewRequest request) {
        if (request == null || request.answerChanges() == null) {
            throw invalidAnswer("Answer changes are required.");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
        requireExamManagement(exam);
        long examVersion = version(exam.getVersion());
        if (omrGradingJobRepository.existsByExamIdAndStatusIn(
                examId, List.of(JobStatus.PENDING, JobStatus.PROCESSING))) {
            throw new BusinessException(ExamErrorCode.EXAM_HAS_ACTIVE_OMR_JOB,
                    "OMR 채점이 끝난 뒤 정답을 변경할 수 있습니다.");
        }
        Map<String, String> proposedKey = new TreeMap<>(exam.getCorrectAnswers());
        Map<String, AnswerChange> changedAnswers = new LinkedHashMap<>();
        for (var entry : new TreeMap<>(request.answerChanges()).entrySet()) {
            int questionNumber = parseQuestionNumber(entry.getKey());
            String key = Integer.toString(questionNumber);
            String proposedAnswer = entry.getValue();
            if (questionNumber > exam.getTotalQuestions()
                    || proposedAnswer == null || proposedAnswer.isBlank() || proposedAnswer.length() > 20
                    || !proposedKey.containsKey(key)) {
                throw invalidAnswer("The proposed answer is invalid for this exam.");
            }
            String currentAnswer = proposedKey.get(key);
            if (!Objects.equals(currentAnswer, proposedAnswer)) {
                changedAnswers.put(key, new AnswerChange(currentAnswer, proposedAnswer));
                proposedKey.put(key, proposedAnswer);
            }
        }

        List<ExamResult> results = resultRepository.findByExamId(examId);
        List<OmrSubmission> submissions = submissionRepository.findSucceededByExamId(examId);
        boolean noOp = changedAnswers.isEmpty();
        String fingerprint = fingerprint(exam, proposedKey, results, submissions);
        if (noOp) {
            return new ExamAnswerRegradePreviewResponse(
                    examId, examVersion, fingerprint, true, Map.of(), 0, 0, 0, Map.of(), Map.of());
        }

        List<ResultProjection> projections = new ArrayList<>(results.size());
        int skipped = 0;
        List<Integer> projectedScores = new ArrayList<>(results.size());
        List<Integer> currentScores = results.stream().map(ExamResult::getTotalScore).toList();
        Map<Integer, Long> scoreImpact = new TreeMap<>();

        for (ExamResult result : results) {
            if (result.getManualOverrideAt() != null) {
                skipped++;
                projectedScores.add(result.getTotalScore());
                projections.add(new ResultProjection(result, null, result.getTotalScore()));
                continue;
            }
            ExamRegradeCalculator.Outcome outcome =
                    calculator.calculate(exam, selectedAnswers(result), proposedKey);
            projections.add(new ResultProjection(result, outcome, outcome.totalScore()));
            projectedScores.add(outcome.totalScore());
            int delta = outcome.totalScore() - result.getTotalScore();
            if (delta != 0) {
                scoreImpact.merge(delta, 1L, Long::sum);
            }
        }

        Map<String, Long> gradeImpact = new TreeMap<>();
        for (ResultProjection projection : projections) {
            ExamResult result = projection.result();
            Integer previousGrade = gradeCalculator.resolveStoredOrRelativeGrade(exam, result, currentScores);
            ExamResult projected = ExamResult.create(exam, result.getStudentId(), projection.projectedScore());
            if (projection.outcome() != null) {
                projected.assignGrade(projection.outcome().storedGrade());
            } else {
                projected.assignGrade(result.getGrade());
            }
            Integer nextGrade = gradeCalculator.resolveStoredOrRelativeGrade(exam, projected, projectedScores);
            if (!Objects.equals(previousGrade, nextGrade)) {
                gradeImpact.merge(gradeLabel(previousGrade) + "->" + gradeLabel(nextGrade), 1L, Long::sum);
            }
        }

        for (OmrSubmission submission : submissions) {
            calculator.calculate(exam, selectedAnswers(submission), proposedKey);
        }

        return new ExamAnswerRegradePreviewResponse(
                examId,
                examVersion,
                fingerprint,
                false,
                Map.copyOf(changedAnswers),
                results.size() - skipped,
                skipped,
                submissions.size(),
                Map.copyOf(scoreImpact),
                Map.copyOf(gradeImpact));
    }

    @Transactional
    public ExamAnswerRegradeRevisionResponse confirm(
            Long examId, ExamAnswerRegradeConfirmRequest request, Long actorUserId) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.answerChanges() == null || request.questionScores() == null
                || request.questionTypes() == null || actorUserId == null) {
            throw invalidAnswer("A correction and staff actor are required.");
        }

        Exam exam = examRepository.findByIdForUpdate(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
        requireExamManagement(exam);
        long beforeVersion = version(exam.getVersion());
        if (omrGradingJobRepository.existsByExamIdAndStatusIn(
                examId, List.of(JobStatus.PENDING, JobStatus.PROCESSING))) {
            throw new BusinessException(ExamErrorCode.EXAM_HAS_ACTIVE_OMR_JOB);
        }
        Map<String, String> proposedKey = new TreeMap<>(exam.getCorrectAnswers());
        Map<String, ExamAnswerKeyRevision.AnswerChange> changedAnswers = new LinkedHashMap<>();
        for (var entry : request.answerChanges().entrySet()) {
            int questionNumber = parseQuestionNumber(entry.getKey());
            String key = Integer.toString(questionNumber);
            String answer = entry.getValue();
            if (questionNumber > exam.getTotalQuestions() || !proposedKey.containsKey(key)
                    || answer == null || answer.isBlank() || answer.length() > 20) {
                throw invalidAnswer("The proposed answer is invalid for this exam.");
            }
            String previous = proposedKey.put(key, answer);
            if (!Objects.equals(previous, answer)) {
                changedAnswers.put(key, new ExamAnswerKeyRevision.AnswerChange(previous, answer));
            }
        }
        Map<String, Integer> proposedScores = new TreeMap<>(request.questionScores());
        Map<String, String> proposedTypes = new TreeMap<>(request.questionTypes());
        validateQuestionConfiguration(exam, proposedKey, proposedScores, proposedTypes);
        Map<String, ExamAnswerKeyRevision.AnswerChange> changedFields = new LinkedHashMap<>();
        String proposedName = request.name().strip();
        addChange(changedFields, "name", exam.getName(), proposedName);
        for (int number = 1; number <= exam.getTotalQuestions(); number++) {
            String key = Integer.toString(number);
            addChange(changedFields, "score:" + key,
                    String.valueOf(exam.getQuestionScores().get(key)), String.valueOf(proposedScores.get(key)));
            addChange(changedFields, "type:" + key,
                    Objects.toString(exam.getQuestionType(number), ""), Objects.toString(proposedTypes.get(key), ""));
        }
        if (changedAnswers.isEmpty() && changedFields.isEmpty()) {
            return ExamAnswerRegradeRevisionResponse.noOp(examId, beforeVersion, Map.of());
        }
        boolean gradingChanged = !changedAnswers.isEmpty() || changedFields.keySet().stream()
                .anyMatch(key -> key.startsWith("score:") || key.startsWith("type:"));
        List<ExamResult> results = gradingChanged ? resultRepository.findByExamId(examId) : List.of();
        List<OmrSubmission> submissions = gradingChanged
                ? submissionRepository.findSucceededByExamId(examId) : List.of();
        List<Integer> currentScores = results.stream().map(ExamResult::getTotalScore).toList();
        List<Integer> projectedScores = new ArrayList<>(results.size());
        IdentityHashMap<ExamResult, ExamRegradeCalculator.Outcome> resultOutcomes = new IdentityHashMap<>();
        int skippedOverrides = 0;

        exam.update(proposedName, null, null, null, null,
                Map.copyOf(proposedKey), Map.copyOf(proposedScores), Map.copyOf(proposedTypes), null);

        for (ExamResult result : results) {
            if (result.getManualOverrideAt() != null) {
                skippedOverrides++;
                projectedScores.add(result.getTotalScore());
                continue;
            }
            ExamRegradeCalculator.Outcome outcome =
                    calculator.calculate(exam, selectedAnswers(result), proposedKey);
            resultOutcomes.put(result, outcome);
            projectedScores.add(outcome.totalScore());
        }

        examRepository.save(exam);
        List<ExamResult> updatedResults = new ArrayList<>(resultOutcomes.size());
        resultOutcomes.forEach((result, outcome) -> {
            List<ExamResult.OmrQuestionUpdate> questionUpdates = outcome.questions().stream()
                    .map(question -> new ExamResult.OmrQuestionUpdate(
                            question.questionNumber(), question.selectedAnswer(),
                            question.correctAnswer(), question.maxScore()))
                    .toList();
            result.regrade(outcome.totalScore(), questionUpdates);
            result.assignGrade(outcome.storedGrade());
            updatedResults.add(result);
        });
        if (!updatedResults.isEmpty()) {
            resultRepository.saveAll(updatedResults);
        }

        for (OmrSubmission submission : submissions) {
            ExamRegradeCalculator.Outcome outcome =
                    calculator.calculate(exam, selectedAnswers(submission), proposedKey);
            Integer grade = exam.isGraded()
                    ? gradeCalculator.calculateGradeForScore(
                            outcome.totalScore(), projectedScores, exam.getGradingType(), exam.getGradeScale())
                    : null;
            List<OmrSubmission.QuestionRegrade> questionUpdates = outcome.questions().stream()
                    .map(question -> new OmrSubmission.QuestionRegrade(
                            question.questionNumber(), question.correctAnswer(), question.maxScore(),
                            exam.getQuestionType(question.questionNumber())))
                    .toList();
            submission.regrade(questionUpdates, grade);
        }
        if (!submissions.isEmpty()) {
            submissionRepository.saveAll(submissions);
        }
        examRepository.flush();

        long afterVersion = version(exam.getVersion());
        ExamAnswerKeyRevision revision = ExamAnswerKeyRevision.create(
                exam, actorUserId, beforeVersion, afterVersion, changedAnswers, changedFields,
                updatedResults.size(), skippedOverrides, submissions.size());
        return ExamAnswerRegradeRevisionResponse.from(revisionRepository.saveAndFlush(revision));
    }

    @Transactional(readOnly = true)
    public Page<ExamAnswerKeyRevisionResponse> history(Long examId, Pageable pageable) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
        requireExamManagement(exam);
        return revisionRepository.findByExamId(examId, pageable)
                .map(ExamAnswerKeyRevisionResponse::from);
    }

    private static void validateQuestionConfiguration(
            Exam exam, Map<String, String> answers, Map<String, Integer> scores, Map<String, String> types) {
        if (answers.size() != exam.getTotalQuestions() || scores.size() != exam.getTotalQuestions()
                || (exam.isGraded() && types.size() != exam.getTotalQuestions())) {
            throw invalidAnswer("Question configuration must cover every exam question.");
        }
        for (int number = 1; number <= exam.getTotalQuestions(); number++) {
            String key = Integer.toString(number);
            String answer = answers.get(key);
            Integer score = scores.get(key);
            String type = types.get(key);
            if (answer == null || answer.isBlank() || answer.length() > 20
                    || score == null || score < 0 || score > 100
                    || (exam.isGraded() && (type == null || type.isBlank() || type.length() > 50))) {
                throw invalidAnswer("Question configuration contains an invalid value.");
            }
        }
        if (exam.isPassFail() && exam.getPassScore() != null
                && exam.getPassScore() > scores.values().stream().mapToInt(Integer::intValue).sum()) {
            throw invalidAnswer("The pass score exceeds the revised total score.");
        }
    }

    private static void addChange(Map<String, ExamAnswerKeyRevision.AnswerChange> changes,
                                  String field, String before, String after) {
        if (!Objects.equals(before, after)) {
            changes.put(field, new ExamAnswerKeyRevision.AnswerChange(before, after));
        }
    }

    private static Map<Integer, String> selectedAnswers(ExamResult result) {
        Map<Integer, String> answers = new TreeMap<>();
        for (QuestionResult question : result.getQuestionResults()) {
            if (answers.containsKey(question.getQuestionNumber())) {
                throw invalidAnswer("A result contains duplicate question numbers.");
            }
            answers.put(question.getQuestionNumber(), question.getSelectedChoice());
        }
        return answers;
    }

    private static Map<Integer, String> selectedAnswers(OmrSubmission submission) {
        Map<Integer, String> answers = new TreeMap<>();
        for (OmrSubmissionQuestion question : submission.getQuestions()) {
            if (answers.containsKey(question.getQuestionNumber())) {
                throw invalidAnswer("An OMR submission contains duplicate question numbers.");
            }
            answers.put(question.getQuestionNumber(), question.getStudentAnswer());
        }
        return answers;
    }

    private static String fingerprint(
            Exam exam,
            Map<String, String> proposedKey,
            List<ExamResult> results,
            List<OmrSubmission> submissions) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, TenantContextHolder.getRequiredTenantId());
        append(canonical, exam.getId());
        append(canonical, version(exam.getVersion()));
        new TreeMap<>(proposedKey).forEach((question, answer) -> {
            append(canonical, question);
            append(canonical, answer);
        });
        results.stream()
                .sorted(Comparator.comparing(result -> Objects.toString(result.getId(), "")))
                .forEach(result -> {
                    append(canonical, "R");
                    append(canonical, result.getId());
                    append(canonical, version(result.getVersion()));
                    append(canonical, result.getTotalScore());
                    append(canonical, result.getManualOverrideAt());
                    result.getQuestionResults().stream()
                            .sorted(Comparator.comparing(QuestionResult::getQuestionNumber))
                            .forEach(question -> {
                                append(canonical, question.getQuestionNumber());
                                append(canonical, question.getSelectedChoice());
                            });
                });
        submissions.stream()
                .sorted(Comparator.comparing(submission -> Objects.toString(submission.getId(), "")))
                .forEach(submission -> {
                    append(canonical, "S");
                    append(canonical, submission.getId());
                    append(canonical, version(submission.getVersion()));
                    append(canonical, submission.getTotalScore());
                    submission.getQuestions().stream()
                            .sorted(Comparator.comparing(OmrSubmissionQuestion::getQuestionNumber))
                            .forEach(question -> {
                                append(canonical, question.getQuestionNumber());
                                append(canonical, question.getStudentAnswer());
                            });
                });
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static int parseQuestionNumber(String question) {
        try {
            int number = Integer.parseInt(question);
            if (number < 1 || !Integer.toString(number).equals(question)) {
                throw invalidAnswer("Question numbers must use canonical positive integers.");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw invalidAnswer("Question numbers must use canonical positive integers.");
        }
    }

    private static void append(StringBuilder target, Object value) {
        String text = Objects.toString(value, "<null>");
        target.append(text.length()).append(':').append(text).append('|');
    }

    private static long version(Long value) {
        return value == null ? 0L : value;
    }

    private static String gradeLabel(Integer grade) {
        return grade == null ? "-" : grade.toString();
    }

    private static void requireExamManagement(Exam exam) {
        if (exam.getSource() != ExamSource.EXAM_MANAGEMENT) {
            throw invalidAnswer("Assignment-owned exams cannot be regraded with this operation.");
        }
    }

    private static BusinessException invalidAnswer(String message) {
        return new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA, message);
    }

    private record ResultProjection(
            ExamResult result, ExamRegradeCalculator.Outcome outcome, int projectedScore) {}
}
