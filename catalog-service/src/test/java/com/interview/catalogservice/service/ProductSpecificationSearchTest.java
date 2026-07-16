package com.interview.catalogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.interview.catalogservice.entities.Category;
import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.CategoryRepository;
import com.interview.catalogservice.repository.ProductRepository;
import com.interview.catalogservice.web.Product;
import com.interview.catalogservice.web.ProductSearchCriteria;

/**
 * Track G / G1 — proves the Specification pattern: each optional filter composes into one query,
 * and unsupplied filters (null) are simply skipped.
 *
 * Seed: Phone(499)+Laptop(1200) in Electronics; "Phone Case"(20) in Accessories.
 */
@DataJpaTest
@Import(ProductService.class)
class ProductSpecificationSearchTest {

	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	TestEntityManager em;

	Long electronicsId;

	@BeforeEach
	void seed() {
		ProductEntity phone = productRepository.save(new ProductEntity("Phone", 499.0));
		ProductEntity laptop = productRepository.save(new ProductEntity("Laptop", 1200.0));
		ProductEntity phoneCase = productRepository.save(new ProductEntity("Phone Case", 20.0));
		Category electronics = categoryRepository.save(new Category("Electronics"));
		Category accessories = categoryRepository.save(new Category("Accessories"));
		electronicsId = electronics.getId();

		productService.assignCategory(phone.getId(), electronics.getId(), 0);
		productService.assignCategory(laptop.getId(), electronics.getId(), 0);
		productService.assignCategory(phoneCase.getId(), accessories.getId(), 0);

		em.flush();
		em.clear();
	}

	@Test
	void nameContains_caseInsensitiveSubstring() {
		List<Product> result = productService.search(new ProductSearchCriteria("phone", null, null, null));
		assertThat(result).extracting(Product::name).containsExactlyInAnyOrder("Phone", "Phone Case");
	}

	@Test
	void priceRange_filtersBothBounds() {
		List<Product> result = productService.search(new ProductSearchCriteria(null, 100.0, 1000.0, null));
		assertThat(result).extracting(Product::name).containsExactly("Phone"); // 20 too low, 1200 too high
	}

	@Test
	void category_usesTheJoin() {
		List<Product> result = productService.search(new ProductSearchCriteria(null, null, null, electronicsId));
		assertThat(result).extracting(Product::name).containsExactlyInAnyOrder("Phone", "Laptop");
	}

	@Test
	void combinedFilters_areAllAnded() {
		// name~"phone" AND price<=100 -> only "Phone Case" (Phone is 499)
		List<Product> result = productService.search(new ProductSearchCriteria("phone", null, 100.0, null));
		assertThat(result).extracting(Product::name).containsExactly("Phone Case");
	}

	@Test
	void noFilters_returnsEverything() {
		List<Product> result = productService.search(new ProductSearchCriteria(null, null, null, null));
		assertThat(result).hasSize(3);
	}
}
