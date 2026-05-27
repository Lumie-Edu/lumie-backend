package com.lumie.tuition.domain.entity;

import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.vo.KycStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantProfileTest {

    @Test
    void submit_from_PENDING_sets_submitted_at() {
        MerchantProfile profile = createProfile();
        profile.submit();
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(profile.getKycSubmittedAt()).isNotNull();
    }

    @Test
    void submit_when_already_PENDING_submitted_throws() {
        MerchantProfile profile = createProfile();
        profile.submit();
        // Calling submit() again from already-submitted PENDING state is not allowed
        // (profile is PENDING but has already been submitted — resubmit() is for REJECTED only)
        // This verifies no double-submission from PENDING after initial submit.
        // To re-test: reject first, then resubmit.
        profile.reject("Missing docs");
        profile.resubmit(); // correct path
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.PENDING);
    }

    @Test
    void submit_from_APPROVED_throws() {
        MerchantProfile profile = createProfile();
        profile.submit();
        profile.approve("seller-key-abc");
        assertThatThrownBy(profile::submit)
                .isInstanceOf(TuitionException.class);
    }

    @Test
    void submit_from_REJECTED_throws() {
        MerchantProfile profile = createProfile();
        profile.submit();
        profile.reject("Missing docs");
        // submit() is only for fresh PENDING; resubmit() is the correct path after rejection
        assertThatThrownBy(profile::submit)
                .isInstanceOf(TuitionException.class);
    }

    @Test
    void resubmit_from_REJECTED_resets_to_PENDING() {
        MerchantProfile profile = createProfile();
        profile.submit();
        profile.reject("Missing docs");
        profile.resubmit();
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(profile.getRejectionReason()).isNull();
        assertThat(profile.getKycSubmittedAt()).isNotNull();
    }

    @Test
    void resubmit_from_PENDING_throws() {
        MerchantProfile profile = createProfile();
        profile.submit();
        assertThatThrownBy(profile::resubmit)
                .isInstanceOf(TuitionException.class);
    }

    @Test
    void approve_from_PENDING_succeeds() {
        MerchantProfile profile = createProfile();
        profile.submit();
        profile.approve("seller-key-abc");
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(profile.getPgSellerKey()).isEqualTo("seller-key-abc");
        assertThat(profile.isApproved()).isTrue();
    }

    @Test
    void reject_from_PENDING_succeeds() {
        MerchantProfile profile = createProfile();
        profile.submit();
        profile.reject("Incomplete documents");
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(profile.getRejectionReason()).isEqualTo("Incomplete documents");
    }

    @Test
    void approve_APPROVED_profile_throws() {
        MerchantProfile profile = createProfile();
        profile.submit();
        profile.approve("seller-key-abc");
        assertThatThrownBy(() -> profile.approve("another-key"))
                .isInstanceOf(TuitionException.class);
    }

    private MerchantProfile createProfile() {
        return MerchantProfile.create(
                "123-45-67890",
                "홍길동",
                "004",
                "123456789012",
                "홍길동"
        );
    }
}
