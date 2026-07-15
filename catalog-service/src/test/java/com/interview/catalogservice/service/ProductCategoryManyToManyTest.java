package com.interview.catalogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.interview.catalogservice.entities.Category;
import com.interview.catalogservice.entities.ProductCategory;
import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.CategoryRepository;
import com.interview.catalogservice.repository.ProductCategoryRepository;
import com.interview.catalogservice.repository.ProductRepository;

/**
 * Track A / A1 — proves the Product<->Category many-to-many (modelled as the ProductCategory
 * link entity with a composite key) works BOTH directions and carries the join attribute.
 *
 * @DataJpaTest = real H2 + JPA + transaction slice; Flyway runs V1/V2/V3 to build the schema;
 * @Import pulls the real ProductService in (it needs the two repositories).
 */
@DataJpaTest
@Import(ProductService.class)
class ProductCategoryManyToManyTest {

	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	ProductCategoryRepository productCategoryRepository;
	@Autowired
	TestEntityManager em;

	@Test
	void product_assignedToTwoCategories_navigatesBothWays_andKeepsPosition() {
		ProductEntity phone = productRepository.save(new ProductEntity("Phone", 499.0));
		Category electronics = categoryRepository.save(new Category("Electronics"));
		Category featured = categoryRepository.save(new Category("Featured"));

		productService.assignCategory(phone.getId(), electronics.getId(), 0);
		productService.assignCategory(phone.getId(), featured.getId(), 1);

		em.flush(); // force the INSERTs
		em.clear(); // detach everything, so reads below hit the DB, not the persistence-context cache

		// direction 1: product -> its categories (the LAZY @OneToMany, loaded inside the tx)
		ProductEntity reloaded = productRepository.findById(phone.getId()).orElseThrow();
		assertThat(reloaded.getCategories()).hasSize(2);
		assertThat(reloaded.getCategories())
				.extracting(pc -> pc.getCategory().getName())
				.containsExactlyInAnyOrder("Electronics", "Featured");

		// direction 2: category -> its products (via the link repo)
		List<ProductCategory> electronicsLinks = productCategoryRepository.findByCategory_Id(electronics.getId());
		assertThat(electronicsLinks).hasSize(1);
		assertThat(electronicsLinks.get(0).getProduct().getName()).isEqualTo("Phone");

		// the join ATTRIBUTE survived the round-trip
		List<ProductCategory> featuredLinks = productCategoryRepository.findByCategory_Id(featured.getId());
		assertThat(featuredLinks).hasSize(1);
		assertThat(featuredLinks.get(0).getPosition()).isEqualTo(1);
	}
}
