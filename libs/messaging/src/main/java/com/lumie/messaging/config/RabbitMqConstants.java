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
    public static final String GRADING_OMR_COMPLETED_QUEUE = "grading.omr-completed";

    // Routing Keys - Grading
    public static final String GRADING_OMR_REQUEST_ROUTING_KEY = "grading.omr.request";
    public static final String GRADING_COMPLETED_ROUTING_KEY = "grading.completed";
}
