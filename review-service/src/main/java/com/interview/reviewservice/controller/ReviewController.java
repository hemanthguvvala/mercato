package com.interview.reviewservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.interview.reviewservice.service.ReviewService;
import com.interview.reviewservice.web.CreateReviewRequest;
import com.interview.reviewservice.web.ReviewResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PostMapping
	public ResponseEntity<ReviewResponse> create(@Valid @RequestBody CreateReviewRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		String reviewer = jwt.getSubject();
		return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(reviewer, request));
	}

	@GetMapping
	public List<ReviewResponse> byProduct(@RequestParam Long productId) {
		return reviewService.byProduct(productId);
	}
}
