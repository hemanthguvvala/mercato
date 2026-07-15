package com.interview.catalogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.ProductRepository;

/**
 * Track A / A3 — proves @ElementCollection tags:
 *  - value strings persist to the product_tag collection table and reload
 *  - it's a Set: re-adding an existing tag across calls does not duplicate
 */
@DataJpaTest
@Import(ProductService.class)
class ProductTagsElementCollectionTest {

	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	TestEntityManager em;

	@Test
	void tags_persistAsValueCollection_andStayUnique() {
		ProductEntity phone = productRepository.save(new ProductEntity("Phone", 499.0));

		productService.addTags(phone.getId(), new LinkedHashSet<>(List.of("new", "featured")));
		em.flush();
		em.clear();

		// "new" already present — Set semantics must not create a duplicate row
		productService.addTags(phone.getId(), new LinkedHashSet<>(List.of("new", "sale")));
		em.flush();
		em.clear();

		ProductEntity reloaded = productRepository.findById(phone.getId()).orElseThrow();
		assertThat(reloaded.getTags()).containsExactlyInAnyOrder("new", "featured", "sale");
	}
}
