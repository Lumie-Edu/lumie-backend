package com.lumie.notification.domain.entity;

import com.lumie.notification.domain.vo.TemplateCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmsTemplate 도메인 엔티티 테스트")
class SmsTemplateTest {

    @Test
    @DisplayName("create() 호출 시 기본 카테고리는 GENERAL이다")
    void create_defaultCategoryIsGeneral() {
        SmsTemplate template = SmsTemplate.create(
                1L, "출석 알림", "{학생명}님이 출석했습니다.", null);

        assertThat(template.getCategory()).isEqualTo(TemplateCategory.GENERAL);
        assertThat(template.getAcademyId()).isEqualTo(1L);
        assertThat(template.getName()).isEqualTo("출석 알림");
        assertThat(template.getContent()).isEqualTo("{학생명}님이 출석했습니다.");
    }

    @Test
    @DisplayName("create() 시 카테고리를 지정하면 해당 카테고리로 생성된다")
    void create_withCategory() {
        SmsTemplate template = SmsTemplate.create(
                1L, "출석 알림", "내용", TemplateCategory.ATTENDANCE);

        assertThat(template.getCategory()).isEqualTo(TemplateCategory.ATTENDANCE);
    }

    @Test
    @DisplayName("updateInfo() 호출 시 필드가 업데이트된다")
    void updateInfo_updatesFields() {
        SmsTemplate template = SmsTemplate.create(
                1L, "원래 이름", "원래 내용", TemplateCategory.GENERAL);

        template.updateInfo("변경된 이름", "변경된 내용", TemplateCategory.EXAM);

        assertThat(template.getName()).isEqualTo("변경된 이름");
        assertThat(template.getContent()).isEqualTo("변경된 내용");
        assertThat(template.getCategory()).isEqualTo(TemplateCategory.EXAM);
    }

    @Test
    @DisplayName("create() 시 모든 카테고리 값으로 생성 가능하다")
    void create_allCategories() {
        for (TemplateCategory category : TemplateCategory.values()) {
            SmsTemplate template = SmsTemplate.create(1L, "이름", "내용", category);
            assertThat(template.getCategory()).isEqualTo(category);
        }
    }
}
