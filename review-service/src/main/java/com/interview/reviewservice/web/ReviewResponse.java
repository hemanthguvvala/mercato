package com.interview.reviewservice.web;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(
		Long id,
		Long productId,
		String reviewerUsername,
		int rating,
		String comment,
		Instant createdAt,
		List<String> imageUrls
		) {

}
