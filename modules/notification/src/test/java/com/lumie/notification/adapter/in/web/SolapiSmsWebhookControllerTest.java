package com.lumie.notification.adapter.in.web;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.notification.application.service.SmsWebhookService;
import com.lumie.notification.domain.exception.SmsErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SolapiSmsWebhookControllerTest {

    @Mock private SmsWebhookService webhookService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SolapiSmsWebhookController(webhookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void receive_continuesProcessingWhenOneEventIsUnmappable() throws Exception {
        when(webhookService.applySolapiReport(anyString(), anyLong(), anyInt(), eq("M_IGNORED"), anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(SmsErrorCode.WEBHOOK_IGNORED));
        when(webhookService.applySolapiReport(anyString(), anyLong(), anyInt(), eq("M_OK"), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/v1/sms/solapi/webhook")
                        .header("X-Solapi-Secret", "secret-sha1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "messageId": "M_IGNORED",
                                    "groupId": "G_TEST",
                                    "statusCode": "4000",
                                    "statusMessage": "done",
                                    "customFields": {
                                      "lumieTenant": "inst-test",
                                      "lumieSmsId": "7",
                                      "lumieRecipient": "0"
                                    }
                                  },
                                  {
                                    "messageId": "M_OK",
                                    "groupId": "G_TEST",
                                    "statusCode": "4000",
                                    "statusMessage": "done",
                                    "customFields": {
                                      "lumieTenant": "inst-test",
                                      "lumieSmsId": "7",
                                      "lumieRecipient": "1"
                                    }
                                  }
                                ]
                                """))
                .andExpect(status().isOk());

        verify(webhookService).verifySecretHash("secret-sha1");
        verify(webhookService).applySolapiReport("inst-test", 7L, 0, "M_IGNORED", "G_TEST", "4000", "done");
        verify(webhookService).applySolapiReport("inst-test", 7L, 1, "M_OK", "G_TEST", "4000", "done");
    }

    @Test
    void receive_rejectsInvalidSecretBeforeProcessingEvents() throws Exception {
        doThrow(new BusinessException(SmsErrorCode.WEBHOOK_UNAUTHORIZED))
                .when(webhookService).verifySecretHash("bad");

        mockMvc.perform(post("/v1/sms/solapi/webhook")
                        .header("X-Solapi-Secret", "bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized());
    }
}
