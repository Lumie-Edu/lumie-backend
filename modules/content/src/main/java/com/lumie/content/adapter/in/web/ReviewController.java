package com.lumie.content.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.content.application.dto.request.CreateReviewRequest;
import com.lumie.content.application.dto.request.UpdateReviewPopupSettingRequest;
import com.lumie.content.application.dto.response.ReviewPopupSettingResponse;
import com.lumie.content.application.dto.response.ReviewResponse;
import com.lumie.content.application.service.ReviewCommandService;
import com.lumie.content.application.service.ReviewPopupSettingService;
import com.lumie.content.application.service.ReviewQueryService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewQueryService queryService;
    private final ReviewCommandService commandService;
    private final ReviewPopupSettingService popupSettingService;

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = commandService.createReview(request);
        return ResponseEntity.created(URI.create("/v1/reviews/" + response.id())).body(response);
    }

    @GetMapping
    @AllowedSortFields({"createdAt"})
    public ResponseEntity<PageResponse<ReviewResponse>> listReviews(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listReviews(pageable)));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
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
