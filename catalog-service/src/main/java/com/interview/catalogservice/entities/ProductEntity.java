package com.interview.catalogservice.entities;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<ProductCategory> categories = new LinkedHashSet<>();

	@ElementCollection
	@CollectionTable(name = "product_tag", joinColumns = @JoinColumn(name = "product_id"))
	@Column(name = "tag")
	private Set<String> tags = new LinkedHashSet<>();

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

	public void addCategory(Category category, int position) {
		categories.add(new ProductCategory(this, category, position));
	}

	public Set<ProductCategory> getCategories() {
		return categories;
	}

	public Set<String> getTags() {
		return tags;
	}
}
