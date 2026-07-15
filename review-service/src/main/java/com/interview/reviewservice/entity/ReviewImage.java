package com.interview.reviewservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_image")
public class ReviewImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(nullable = false)
	private String url;

	protected ReviewImage() {
	}

	public ReviewImage(Review review, String url) {
		this.review = review;
		this.url = url;
	}

	public Long getId() {
		return id;
	}

	public Review getReview() {
		return review;
	}

	public String getUrl() {
		return url;
	}

}
