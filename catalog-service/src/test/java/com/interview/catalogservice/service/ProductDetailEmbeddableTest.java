package com.interview.catalogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.interview.catalogservice.entities.Dimensions;
import com.interview.catalogservice.entities.ProductDetail;
import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.ProductDetailRepository;
import com.interview.catalogservice.repository.ProductRepository;

/**
 * Track A / A4 — proves the @Embeddable Dimensions value object inlines into the
 * product_detail row (no separate table/join) and round-trips intact.
 */
@DataJpaTest
@Import(ProductService.class)
class ProductDetailEmbeddableTest {

	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	ProductDetailRepository productDetailRepository;
	@Autowired
	TestEntityManager em;

	@Test
	void dimensions_embedInlineIntoProductDetail_andReloadIntact() {
		ProductEntity phone = productRepository.save(new ProductEntity("Phone", 499.0));
		productService.upsertDetail(phone.getId(), "A great phone");
		productService.setDimensions(phone.getId(), new Dimensions(15.0, 7.5, 0.8));

		em.flush();
		em.clear();

		ProductDetail detail = productDetailRepository.findById(phone.getId()).orElseThrow();
		assertThat(detail.getDimensions()).isNotNull();
		assertThat(detail.getDimensions().getLengthCm()).isEqualTo(15.0);
		assertThat(detail.getDimensions().getWidthCm()).isEqualTo(7.5);
		assertThat(detail.getDimensions().getHeightCm()).isEqualTo(0.8);
	}
}
