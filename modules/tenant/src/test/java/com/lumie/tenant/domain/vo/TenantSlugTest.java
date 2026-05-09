package com.lumie.tenant.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantSlugTest {

    @Test
    @DisplayName("유효한 slug 생성")
    void createValidSlug() {
        TenantSlug slug = TenantSlug.of("my-academy");

        assertThat(slug.getValue()).isEqualTo("my-academy");
        assertThat(slug.toSchemaName()).isEqualTo("tenant_my_academy");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "my-academy", "test123", "a12-345-678"})
    @DisplayName("유효한 slug 패턴 검증")
    void validSlugPatterns(String value) {
        TenantSlug slug = TenantSlug.of(value);
        assertThat(slug.getValue()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "123abc", "-abc", "ABC", "my_academy", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    @DisplayName("무효한 slug 패턴 거부")
    void invalidSlugPatterns(String value) {
        assertThatThrownBy(() -> TenantSlug.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid tenant slug");
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "api", "www", "login", "dashboard"})
    @DisplayName("예약된 slug 거부")
    void reservedSlugs(String value) {
        assertThatThrownBy(() -> TenantSlug.of(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("slug 대소문자 정규화")
    void slugNormalization() {
        TenantSlug slug = TenantSlug.of("myacademy");
        assertThat(slug.getValue()).isEqualTo("myacademy");
    }
}
