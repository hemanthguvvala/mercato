package com.interview.catalogservice.repository;

import org.springframework.data.jpa.domain.Specification;

import com.interview.catalogservice.entities.ProductEntity;

public class ProductSpecifications {

	private ProductSpecifications() {
	}

	public static Specification<ProductEntity> nameContains(String term) {
		return (root, query, cb) -> cb.like(cb.lower(root.<String>get("name")), "%" + term.toLowerCase() + "%");
	}

	public static Specification<ProductEntity> priceAtLeast(double min) {
		return (root, query, cb) -> cb.ge(root.<Number>get("price"), min);
	}

	public static Specification<ProductEntity> priceAtMost(double max) {
		return (root, query, cb) -> cb.le(root.<Number>get("price"), max);
	}

	public static Specification<ProductEntity> inCategory(Long categoryId) {
		return (root, query, cb) -> {
			query.distinct(true);
			var pc = root.join("categories");
			return cb.equal(pc.get("category").get("id"), categoryId);
		};
	}
}
