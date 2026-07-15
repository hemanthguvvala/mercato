package com.interview.reviewservice.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "review")
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long productId;
	@Column(nullable = false)
	private String reviewerUsername;

	@Column(nullable = false)
	private int rating;

	@Column(length = 2000)
	private String comment;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReviewImage> images = new ArrayList<>();

	protected Review() {
	}

	public Review(Long productId, String reviwerUsername, int rating, String comment) {
		this.productId = productId;
		this.reviewerUsername = reviwerUsername;
		this.rating = rating;
		this.comment = comment;
	}

	public void addImage(String url) {
		images.add(new ReviewImage(this, url));
	}

	public Long getId() {
		return id;
	}

	public Long getProductId() {
		return productId;
	}

	public String getReviewerUsername() {
		return reviewerUsername;
	}

	public int getRating() {
		return rating;
	}

	public String getComment() {
		return comment;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<ReviewImage> getImages() {
		return images;
	}
}
