package com.interview.reviewservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.interview.reviewservice.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

	List<Review> findByProductId(Long productId);
	
	@Query("select distinct r from Review r left join fetch r.images where r.productId = :productId")
	List<Review> findByProductIdWithImages(@Param("productId") Long productId);
}
