package com.lumie.exam.application.port.out;

public interface ReportServicePort {

    /**
     * Generate exam report for a student
     *
     * @param studentId Student ID
     * @param examId Exam ID
     * @param tenantSlug Tenant slug
     * @return JPG image bytes
     */
    byte[] generateReport(Long studentId, Long examId, String tenantSlug);
}
