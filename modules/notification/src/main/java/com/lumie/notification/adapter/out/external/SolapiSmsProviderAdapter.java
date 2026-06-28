package com.lumie.notification.adapter.out.external;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.logging.ExternalCallLoggingInterceptor;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.port.out.SmsProviderPort;
import com.lumie.notification.domain.exception.SmsErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class SolapiSmsProviderAdapter implements SmsProviderPort {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final SmsProperties properties;
    private final RestClient restClient;

    public SolapiSmsProviderAdapter(SmsProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(properties.solapi().baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(new ExternalCallLoggingInterceptor())
                .build();
    }

    @Override
    public String providerName() {
        return "solapi";
    }

    @Override
    public void validateConfiguration() {
        if (isBlank(properties.solapi().apiKey())
                || isBlank(properties.solapi().apiSecret())
                || isBlank(properties.solapi().webhookToken())) {
            throw new BusinessException(
                    SmsErrorCode.PROVIDER_CONFIGURATION_INVALID,
                    "SOLAPI requires SOLAPI_API_KEY, SOLAPI_API_SECRET, and SOLAPI_WEBHOOK_TOKEN"
            );
        }
    }

    @Override
    public SendResult send(SendCommand command) {
        validateConfiguration();
        if (isBlank(command.from())) {
            throw new BusinessException(SmsErrorCode.PROVIDER_SEND_FAILED, "SOLAPI sender number is required");
        }
        SolapiSendRequest body = new SolapiSendRequest(
                command.recipients().stream()
                        .map(recipient -> new SolapiMessage(
                                recipient.to(),
                                command.from(),
                                recipient.text(),
                                command.messageType().name(),
                                command.messageType().name().equals("LMS") ? "Lumie" : null,
                                Map.of(
                                        "lumieTenant", command.tenantSlug(),
                                        "lumieSmsId", String.valueOf(command.messageId()),
                                        "lumieRecipient", String.valueOf(recipient.index())
                                )
                        ))
                        .toList(),
                false,
                false,
                true
        );

        try {
            SolapiSendResponse response = restClient.post()
                    .uri("/messages/v4/send-many/detail")
                    .header("Authorization", authorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(), (request, result) -> {
                        throw new BusinessException(
                                SmsErrorCode.PROVIDER_SEND_FAILED,
                                "SOLAPI send failed with HTTP status " + result.getStatusCode()
                        );
                    })
                    .body(SolapiSendResponse.class);

            if (response == null) {
                throw new BusinessException(SmsErrorCode.PROVIDER_SEND_FAILED, "SOLAPI send returned an empty body");
            }

            List<RecipientResult> recipients = response.messageList() == null
                    ? command.recipients().stream()
                            .map(recipient -> new RecipientResult(recipient.index(), null, "2000", "ACCEPTED"))
                            .toList()
                    : response.messageList().stream()
                            .map(item -> {
                                int index = parseRecipientIndex(item.customFields());
                                return new RecipientResult(
                                        index,
                                        item.messageId(),
                                        item.statusCode(),
                                        item.statusMessage()
                                );
                            })
                            .toList();

            log.info("SOLAPI SMS accepted: lumieMessageId={}, providerGroupId={}, recipients={}",
                    command.messageId(), response.groupId(), recipients.size());
            return new SendResult(providerName(), response.groupId(), recipients);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SmsErrorCode.PROVIDER_SEND_FAILED, "SOLAPI send failed: " + e.getMessage(), e);
        }
    }

    String authorizationHeader() {
        String dateTime = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        String signature = signature(properties.solapi().apiSecret(), dateTime, salt);
        return "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
                .formatted(properties.solapi().apiKey(), dateTime, salt, signature);
    }

    private String signature(String apiSecret, String dateTime, String salt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((dateTime + salt).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(SmsErrorCode.PROVIDER_CONFIGURATION_INVALID, "Failed to create SOLAPI signature", e);
        }
    }

    private int parseRecipientIndex(Map<String, String> customFields) {
        if (customFields == null || customFields.get("lumieRecipient") == null) {
            return -1;
        }
        try {
            return Integer.parseInt(customFields.get("lumieRecipient"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record SolapiSendRequest(
            List<SolapiMessage> messages,
            boolean strict,
            boolean allowDuplicates,
            boolean showMessageList
    ) {
    }

    record SolapiMessage(
            String to,
            String from,
            String text,
            String type,
            String subject,
            Map<String, String> customFields
    ) {
    }

    record SolapiSendResponse(
            String groupId,
            List<SolapiMessageResult> messageList
    ) {
    }

    record SolapiMessageResult(
            String messageId,
            String statusCode,
            String statusMessage,
            Map<String, String> customFields
    ) {
    }
}
