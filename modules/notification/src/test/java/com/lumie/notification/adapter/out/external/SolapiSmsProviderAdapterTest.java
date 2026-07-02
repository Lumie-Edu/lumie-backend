package com.lumie.notification.adapter.out.external;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.port.out.SmsProviderPort;
import com.lumie.notification.domain.vo.SmsMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class SolapiSmsProviderAdapterTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void send_postsSendManyDetailWithHmacAuthorizationAndCustomFields() {
        wm.stubFor(post(urlEqualTo("/messages/v4/send-many/detail"))
                .withHeader("Authorization", matching("HMAC-SHA256 apiKey=test-key, date=.+, salt=.+, signature=[0-9a-f]+"))
                .withRequestBody(matchingJsonPath("$.showMessageList", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.messages[0].to", equalTo("01012345678")))
                .withRequestBody(matchingJsonPath("$.messages[0].from", equalTo("0299988888")))
                .withRequestBody(matchingJsonPath("$.messages[0].subject", absent()))
                .withRequestBody(matchingJsonPath("$.messages[0].customFields.lumieTenant", equalTo("inst-test")))
                .withRequestBody(matchingJsonPath("$.messages[0].customFields.lumieSmsId", equalTo("7")))
                .withRequestBody(matchingJsonPath("$.messages[0].customFields.lumieRecipient", equalTo("0")))
                .willReturn(okJson("""
                        {
                          "groupId": "G4V_TEST",
                          "messageList": [
                            {
                              "messageId": "M_TEST_1",
                              "statusCode": "2000",
                              "statusMessage": "정상 접수",
                              "customFields": {
                                "lumieRecipient": "0"
                              }
                            }
                          ]
                        }
                        """)));

        SolapiSmsProviderAdapter adapter = new SolapiSmsProviderAdapter(new SmsProperties(
                "solapi",
                50,
                3,
                new SmsProperties.Solapi("test-key", "test-secret", wm.baseUrl(), "webhook-token")
        ));

        SmsProviderPort.SendResult result = adapter.send(new SmsProviderPort.SendCommand(
                7L,
                "inst-test",
                "0299988888",
                SmsMessageType.SMS,
                List.of(new SmsProviderPort.RecipientCommand(0, "01012345678", "안내 문자"))
        ));

        assertThat(result.provider()).isEqualTo("solapi");
        assertThat(result.providerGroupId()).isEqualTo("G4V_TEST");
        assertThat(result.recipients()).singleElement().satisfies(recipient -> {
            assertThat(recipient.index()).isZero();
            assertThat(recipient.providerMessageId()).isEqualTo("M_TEST_1");
            assertThat(recipient.statusCode()).isEqualTo("2000");
        });
    }

    @Test
    void send_omitsSubjectForLms() {
        wm.stubFor(post(urlEqualTo("/messages/v4/send-many/detail"))
                .withRequestBody(matchingJsonPath("$.messages[0].type", equalTo("LMS")))
                .withRequestBody(matchingJsonPath("$.messages[0].subject", absent()))
                .willReturn(okJson("""
                        {
                          "groupId": "G4V_LMS",
                          "messageList": [
                            {
                              "messageId": "M_LMS_1",
                              "statusCode": "2000",
                              "statusMessage": "정상 접수",
                              "customFields": {
                                "lumieRecipient": "0"
                              }
                            }
                          ]
                        }
                        """)));

        SolapiSmsProviderAdapter adapter = new SolapiSmsProviderAdapter(new SmsProperties(
                "solapi",
                50,
                3,
                new SmsProperties.Solapi("test-key", "test-secret", wm.baseUrl(), "webhook-token")
        ));

        SmsProviderPort.SendResult result = adapter.send(new SmsProviderPort.SendCommand(
                8L,
                "inst-test",
                "0299988888",
                SmsMessageType.LMS,
                List.of(new SmsProviderPort.RecipientCommand(0, "01012345678", "긴 안내 문자"))
        ));

        assertThat(result.providerGroupId()).isEqualTo("G4V_LMS");
    }
}
