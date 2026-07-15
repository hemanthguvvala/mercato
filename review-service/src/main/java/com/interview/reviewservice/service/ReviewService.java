package com.interview.reviewservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.reviewservice.entity.Review;
import com.interview.reviewservice.entity.ReviewImage;
import com.interview.reviewservice.repository.ReviewRepository;
import com.interview.reviewservice.web.CreateReviewRequest;
import com.interview.reviewservice.web.ReviewResponse;

@Service
public class ReviewService {

	private final ReviewRepository reviewRepository;

	public ReviewService(ReviewRepository reviewRepository) {
		this.reviewRepository = reviewRepository;
	}

	@Transactional
	public ReviewResponse create(String reviewUsername, CreateReviewRequest request) {
		Review review = new Review(request.productId(), reviewUsername, request.rating(), request.comment());
		if (request.imageUrls() != null) {
			request.imageUrls().forEach(review::addImage);
		}

		return toResponse(reviewRepository.save(review));
	}

	@Transactional(readOnly = true)
	public List<ReviewResponse> byProduct(Long productId) {
		return reviewRepository.findByProductIdWithImages(productId).stream().map(this::toResponse).toList();
	}

	private ReviewResponse toResponse(Review r) {
		List<String> urls = r.getImages().stream().map(ReviewImage::getUrl).toList();
		return new ReviewResponse(r.getId(), r.getProductId(), r.getReviewerUsername(), r.getRating(), r.getComment(),
				r.getCreatedAt(), urls);
	}
}
