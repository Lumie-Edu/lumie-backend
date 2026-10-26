package com.lumie.file.adapter.in.messaging;

import com.lumie.file.application.dto.message.UploadSessionCleanupMessage;
import com.lumie.file.application.service.UploadSessionCleanupService;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UploadSessionCleanupListener {
    private final UploadSessionCleanupService cleanupService;

    @RabbitListener(queues = RabbitMqConstants.FILE_UPLOAD_CLEANUP_QUEUE)
    public void onCleanup(UploadSessionCleanupMessage message) {
        cleanupService.clean(message);
    }
}
