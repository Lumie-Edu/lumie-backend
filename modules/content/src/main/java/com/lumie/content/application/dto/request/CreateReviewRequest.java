package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 200, message = "제목은 200자 이하로 입력해주세요")
        String reviewTitle,

        @NotBlank(message = "내용을 입력해주세요")
        @Size(max = 1000, message = "내용은 1000자 이하로 입력해주세요")
        String reviewContent
) {
}
