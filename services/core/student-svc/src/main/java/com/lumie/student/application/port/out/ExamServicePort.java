package com.lumie.student.application.port.out;

/**
 * Output port for exam-svc in-process internal API.
 */
public interface ExamServicePort {

    /**
     * Links previously unregistered exam results matching the given phone number
     * to a newly registered student. Silently no-ops when nothing matches.
     *
     * @param phoneNumber phone number recorded on unregistered exam results
     * @param studentId   student id to backfill
     * @return number of rows updated
     */
    int linkUnregisteredResultsByPhone(String phoneNumber, Long studentId);
}
