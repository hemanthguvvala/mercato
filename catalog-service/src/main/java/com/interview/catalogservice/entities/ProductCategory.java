package com.interview.catalogservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_category")
public class ProductCategory {

	@EmbeddedId
	private ProductCategoryId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("productId")
	@JoinColumn(name = "product_id")
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("categoryId")
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(nullable = false)
	private int position;

	protected ProductCategory() {
	}

	public ProductCategory(ProductEntity product, Category category, int position) {
		this.product = product;
		this.category = category;
		this.id = new ProductCategoryId(product.getId(), category.getId());
		this.position = position;
	}

	public ProductCategoryId getId() {
		return id;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public Category getCategory() {
		return category;
	}

	public int getPosition() {
		return position;
	}
}
