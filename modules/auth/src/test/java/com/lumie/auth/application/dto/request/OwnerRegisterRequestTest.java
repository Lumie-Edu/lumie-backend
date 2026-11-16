package com.lumie.auth.application.dto.request;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerRegisterRequestTest {

    @Test
    void missingOrEmptySubjectsUseLegacyEnglishDefault() {
        OwnerRegisterRequest missing = OwnerRegisterRequest.builder()
                .userLoginId("owner-login")
                .password("StrongPass1!")
                .name("Academy Owner")
                .instituteName("Academy")
                .subjects(null)
                .build();
        OwnerRegisterRequest empty = OwnerRegisterRequest.builder()
                .userLoginId("owner-login")
                .password("StrongPass1!")
                .name("Academy Owner")
                .instituteName("Academy")
                .subjects(List.of())
                .build();

        assertThat(missing.subjects()).containsExactly("ENGLISH");
        assertThat(empty.subjects()).containsExactly("ENGLISH");
    }

    @Test
    void selectedSubjectsArePreserved() {
        OwnerRegisterRequest request = OwnerRegisterRequest.builder()
                .userLoginId("owner-login")
                .password("StrongPass1!")
                .name("Academy Owner")
                .instituteName("Academy")
                .subjects(List.of("KOREAN", "MATH"))
                .build();

        assertThat(request.subjects()).containsExactly("KOREAN", "MATH");
    }
}
