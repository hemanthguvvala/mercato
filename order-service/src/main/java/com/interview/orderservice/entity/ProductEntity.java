package com.interview.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product")
public class ProductEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String name;

	@Column(nullable = false)
	private double price;

	@Version
	private Long version;

	public ProductEntity() {
	}

	public ProductEntity(String name, double price) {
		this.name = name;
		this.price = price;
	}

	public ProductEntity(Long id, String name, double price, Long version) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.version = version;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getPrice() {
		return price;
	}
	
	public Long getVersion() {
		return version;
	}

}
