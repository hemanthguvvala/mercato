package com.interview.catalogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.interview.catalogservice.entities.ProductDetail;
import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.ProductDetailRepository;
import com.interview.catalogservice.repository.ProductRepository;

/**
 * Track A / A2 — proves the Product<->ProductDetail one-to-one:
 *  - ProductDetail's primary key IS the product's id (shared PK via @MapsId)
 *  - a second upsert UPDATES the same row (the 1:1 holds — no duplicate)
 */
@DataJpaTest
@Import(ProductService.class)
class ProductDetailOneToOneTest {

	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	ProductDetailRepository productDetailRepository;
	@Autowired
	TestEntityManager em;

	@Test
	void detail_sharesProductPrimaryKey_andUpsertDoesNotDuplicate() {
		ProductEntity phone = productRepository.save(new ProductEntity("Phone", 499.0));

		productService.upsertDetail(phone.getId(), "A great phone");
		em.flush();
		em.clear();

		ProductDetail detail = productDetailRepository.findById(phone.getId()).orElseThrow();
		assertThat(detail.getId()).isEqualTo(phone.getId());              // shared PK
		assertThat(detail.getProduct().getId()).isEqualTo(phone.getId()); // FK == PK
		assertThat(detail.getDescription()).isEqualTo("A great phone");

		// second upsert -> update in place, NOT a new row
		productService.upsertDetail(phone.getId(), "Even better phone");
		em.flush();
		em.clear();

		assertThat(productDetailRepository.count()).isEqualTo(1);
		assertThat(productDetailRepository.findById(phone.getId()).orElseThrow().getDescription())
				.isEqualTo("Even better phone");
	}
}
