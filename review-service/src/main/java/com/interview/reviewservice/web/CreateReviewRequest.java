package com.interview.reviewservice.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
		@NotNull Long productId,
		@Min(1) @Max(5) int rating,
		@Size(max = 2000) String comment,
		java.util.List<@NotBlank String> imageUrls
		) {

}
