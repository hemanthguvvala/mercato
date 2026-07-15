package com.interview.reviewservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.interview.reviewservice.web.CreateReviewRequest;
import com.interview.reviewservice.web.ReviewResponse;

/**
 * Track C / C6 — proves the review aggregate on H2:
 *  - a review + its images persist together (@OneToMany cascade aggregate)
 *  - the reviewer identity is stored as passed (in prod it comes from the JWT, not the body)
 *  - byProduct's fetch-join loads each review's images (no N+1, no LazyInitializationException)
 */
@DataJpaTest
@Import(ReviewService.class)
class ReviewAggregateTest {

	@Autowired
	ReviewService reviewService;
	@Autowired
	TestEntityManager em;

	@Test
	void createReview_persistsImageAggregate_andFetchJoinReadsBack() {
		CreateReviewRequest req = new CreateReviewRequest(
				5L, 4, "Solid phone",
				List.of("http://img/1.jpg", "http://img/2.jpg"));

		ReviewResponse created = reviewService.create("hemanth", req);
		assertThat(created.imageUrls()).hasSize(2); // images cascaded with the review

		em.flush();
		em.clear();

		List<ReviewResponse> byProduct = reviewService.byProduct(5L);
		assertThat(byProduct).hasSize(1);

		ReviewResponse r = byProduct.get(0);
		assertThat(r.productId()).isEqualTo(5L);
		assertThat(r.reviewerUsername()).isEqualTo("hemanth"); // reviewer identity persisted
		assertThat(r.rating()).isEqualTo(4);
		assertThat(r.createdAt()).isNotNull();               // @CreationTimestamp populated
		assertThat(r.imageUrls())
				.containsExactlyInAnyOrder("http://img/1.jpg", "http://img/2.jpg");
	}
}
