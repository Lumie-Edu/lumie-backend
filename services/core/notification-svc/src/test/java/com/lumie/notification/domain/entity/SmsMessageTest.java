package com.lumie.notification.domain.entity;

import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmsMessage 도메인 엔티티 테스트")
class SmsMessageTest {

    @Test
    @DisplayName("create() 호출 시 기본 상태는 PENDING이다")
    void create_defaultStatusIsPending() {
        SmsMessage message = SmsMessage.create(
                10L, RecipientType.STUDENT,
                List.of(new Recipient("010-1234-5678", "홍길동", 100L)),
                "테스트 제목", "테스트 내용", null);

        assertThat(message.getStatus()).isEqualTo(SmsStatus.PENDING);
        assertThat(message.getSenderId()).isEqualTo(10L);
        assertThat(message.getRecipientType()).isEqualTo(RecipientType.STUDENT);
        assertThat(message.getRecipients()).hasSize(1);
        assertThat(message.getTitle()).isEqualTo("테스트 제목");
        assertThat(message.getContent()).isEqualTo("테스트 내용");
        assertThat(message.getSuccessCount()).isZero();
        assertThat(message.getFailCount()).isZero();
    }

    @Test
    @DisplayName("markAsSent() 호출 시 상태가 SENT로 변경된다")
    void markAsSent_statusChangesToSent() {
        SmsMessage message = buildMessage();

        message.markAsSent();

        assertThat(message.getStatus()).isEqualTo(SmsStatus.SENT);
        assertThat(message.getSentAt()).isNotNull();
        assertThat(message.getSuccessCount()).isEqualTo(2);
        assertThat(message.getFailCount()).isZero();
    }

    @Test
    @DisplayName("markAsFailed() 호출 시 상태가 FAILED로 변경된다")
    void markAsFailed_statusChangesToFailed() {
        SmsMessage message = buildMessage();

        message.markAsFailed();

        assertThat(message.getStatus()).isEqualTo(SmsStatus.FAILED);
        assertThat(message.getSentAt()).isNotNull();
        assertThat(message.getSuccessCount()).isZero();
        assertThat(message.getFailCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("markAsPartial() 호출 시 상태가 PARTIAL로 변경된다")
    void markAsPartial_statusChangesToPartial() {
        SmsMessage message = buildMessage();

        message.markAsPartial(1, 1);

        assertThat(message.getStatus()).isEqualTo(SmsStatus.PARTIAL);
        assertThat(message.getSentAt()).isNotNull();
        assertThat(message.getSuccessCount()).isEqualTo(1);
        assertThat(message.getFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("isPending()은 PENDING 상태일 때만 true를 반환한다")
    void isPending_returnsTrueOnlyWhenPending() {
        SmsMessage message = buildMessage();
        assertThat(message.isPending()).isTrue();

        message.markAsSent();
        assertThat(message.isPending()).isFalse();
    }

    @Test
    @DisplayName("create() 시 templateId가 null이어도 생성 가능하다")
    void create_nullTemplateId_succeeds() {
        SmsMessage message = SmsMessage.create(
                10L, RecipientType.CUSTOM,
                List.of(new Recipient("010-0000-0000", "테스트", null)),
                null, "내용", null);

        assertThat(message.getTemplateId()).isNull();
        assertThat(message.getTitle()).isNull();
        assertThat(message.getRecipientType()).isEqualTo(RecipientType.CUSTOM);
    }

    private SmsMessage buildMessage() {
        return SmsMessage.create(
                10L, RecipientType.STUDENT,
                List.of(
                    new Recipient("010-1234-5678", "홍길동", 100L),
                    new Recipient("010-9876-5432", "김철수", 200L)
                ),
                "테스트 제목", "테스트 내용", null);
    }
}
