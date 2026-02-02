package com.lumie.content.adapter.in.web;

import com.lumie.content.application.dto.request.UpdateReviewPopupSettingRequest;
import com.lumie.content.application.dto.response.ReviewPopupSettingResponse;
import com.lumie.content.application.dto.response.ReviewResponse;
import com.lumie.content.application.service.ReviewCommandService;
import com.lumie.content.application.service.ReviewPopupSettingService;
import com.lumie.content.application.service.ReviewQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewQueryService queryService;
    private final ReviewCommandService commandService;
    private final ReviewPopupSettingService popupSettingService;

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> listReviews(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listReviews(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        commandService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popup-setting")
    public ResponseEntity<ReviewPopupSettingResponse> getPopupSetting() {
        return ResponseEntity.ok(popupSettingService.getPopupSetting());
    }

    @PutMapping("/popup-setting")
    public ResponseEntity<ReviewPopupSettingResponse> updatePopupSetting(
            @Valid @RequestBody UpdateReviewPopupSettingRequest request) {
        return ResponseEntity.ok(popupSettingService.updatePopupSetting(request));
    }
}
