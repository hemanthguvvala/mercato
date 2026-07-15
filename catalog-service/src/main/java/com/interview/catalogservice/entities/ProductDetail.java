package com.interview.catalogservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_detail")
public class ProductDetail {

	@Id
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "id")
	private ProductEntity product;

	@Column(length = 2000)
	private String description;

	@Embedded
	private Dimensions dimensions;

	protected ProductDetail() {
	}

	public ProductDetail(ProductEntity product, String description) {
		this.product = product;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Dimensions getDimensions() {
		return dimensions;
	}

	public void setDimensions(Dimensions dimensions) {
		this.dimensions = dimensions;
	}
}
