package com.lumie.messaging.config;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    // Exchanges
    public static final String LUMIE_COMMANDS_EXCHANGE = "lumie.commands";
    public static final String LUMIE_DLX_EXCHANGE = "lumie.dlx";

    // Queues - Grading
    public static final String GRADING_OMR_REQUEST_QUEUE = "grading.omr-request";
    public static final String GRADING_OMR_REQUEST_DLQ = "grading.omr-request.dlq";
    public static final String GRADING_OMR_CALLBACK_QUEUE = "grading.omr-callback";
    public static final String GRADING_OMR_CALLBACK_DLQ = "grading.omr-callback.dlq";

    // Routing Keys - Grading
    public static final String GRADING_OMR_REQUEST_ROUTING_KEY = "grading.omr.request";
    public static final String GRADING_OMR_CALLBACK_ROUTING_KEY = "grading.omr.callback";

    // Queues - Report
    public static final String REPORT_REQUEST_QUEUE = "report.generation-request";
    public static final String REPORT_REQUEST_DLQ = "report.generation-request.dlq";
    public static final String REPORT_CALLBACK_QUEUE = "report.generation-callback";
    public static final String REPORT_CALLBACK_DLQ = "report.generation-callback.dlq";

    // Routing Keys - Report
    public static final String REPORT_REQUEST_ROUTING_KEY = "report.generation.request";
    public static final String REPORT_CALLBACK_ROUTING_KEY = "report.generation.callback";

    // Upload-session cleanup uses a TTL delay queue and a durable consumer queue.
    public static final String FILE_UPLOAD_CLEANUP_QUEUE = "file.upload-session-cleanup";
    public static final String FILE_UPLOAD_CLEANUP_DELAY_QUEUE = "file.upload-session-cleanup.delay";
    public static final String FILE_UPLOAD_CLEANUP_DLQ = "file.upload-session-cleanup.dlq";
    public static final String FILE_UPLOAD_CLEANUP_ROUTING_KEY = "file.upload-session.cleanup";
    public static final String FILE_UPLOAD_CLEANUP_DELAY_ROUTING_KEY = "file.upload-session.cleanup.delay";

}
