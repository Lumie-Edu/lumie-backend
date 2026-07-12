package com.lumie.tenant.domain.vo;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.util.SlugValidator;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class TenantSlug {

    @Column(name = "slug", nullable = false, unique = true, length = 30)
    private String value;

    private TenantSlug(String value) {
        this.value = value;
    }

    public static TenantSlug of(String value) {
        if (!SlugValidator.isValid(value)) {
            throw new BusinessException(TenantErrorCode.INVALID_SLUG,
                    "Invalid tenant slug: " + value);
        }
        return new TenantSlug(value.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return value;
    }
}
