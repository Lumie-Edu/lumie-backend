package com.lumie.internal.exam;

/**
 * In-process internal API exposed by exam-svc and consumed by other services.
 */
public interface ExamService {

    /**
     * Links previously unregistered exam results (student_id IS NULL, matching phone_number)
     * to a newly registered student. Runs within the current tenant scope.
     *
     * @param phoneNumber the phone number recorded on unregistered exam results
     * @param studentId   the student id to backfill
     * @return number of exam_results rows updated
     */
    int linkUnregisteredResultsByPhone(String phoneNumber, Long studentId);
}
