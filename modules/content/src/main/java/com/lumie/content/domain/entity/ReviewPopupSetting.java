package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "toggles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewPopupSetting extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "is_review_popup_on", nullable = false)
    private Boolean isReviewPopupOn;

    public ReviewPopupSetting(Long id, Boolean isReviewPopupOn) {
        this.id = id != null ? id : 1L;
        this.isReviewPopupOn = isReviewPopupOn != null ? isReviewPopupOn : true;
    }

    public void updateIsReviewPopupOn(Boolean isReviewPopupOn) {
        if (isReviewPopupOn != null) {
            this.isReviewPopupOn = isReviewPopupOn;
        }
    }
}
