package com.interview.catalogservice.entities;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class ProductCategoryId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long productId;
	private Long categoryId;

	protected ProductCategoryId() {
	}

	public ProductCategoryId(Long productId, Long categoryId) {
		this.categoryId = categoryId;
		this.productId = productId;
	}

	public Long getProductId() {
		return productId;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categoryId, productId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ProductCategoryId that))
			return false;
		return Objects.equals(categoryId, that.categoryId) && Objects.equals(productId, that.productId);
	}

}
