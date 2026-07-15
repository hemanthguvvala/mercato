package com.interview.catalogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.interview.catalogservice.entities.Category;
import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.CategoryRepository;
import com.interview.catalogservice.repository.ProductRepository;
import com.interview.catalogservice.web.ProductWithCategories;

import jakarta.persistence.EntityManagerFactory;

/**
 * Track A / A5 — proves the fetch-join kills the N+1: the product + its category links +
 * their categories all load in EXACTLY ONE SQL query (counted via Hibernate Statistics),
 * and the result is projected to a DTO (never the entity).
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(ProductService.class)
class ProductFetchJoinNPlusOneTest {

	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	TestEntityManager em;
	@Autowired
	EntityManagerFactory emf;

	@Test
	void getWithCategories_loadsWholeGraphInOneQuery() {
		ProductEntity phone = productRepository.save(new ProductEntity("Phone", 499.0));
		Category electronics = categoryRepository.save(new Category("Electronics"));
		Category featured = categoryRepository.save(new Category("Featured"));
		productService.assignCategory(phone.getId(), electronics.getId(), 0);
		productService.assignCategory(phone.getId(), featured.getId(), 1);
		em.flush();
		em.clear();

		Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
		stats.clear(); // reset counters AFTER all the setup writes

		Optional<ProductWithCategories> result = productService.getWithCategories(phone.getId());

		// behaviour: DTO fully populated, both categories present
		assertThat(result).isPresent();
		assertThat(result.get().categories())
				.extracting(ProductWithCategories.CategoryView::name)
				.containsExactlyInAnyOrder("Electronics", "Featured");

		// the N+1 proof: the whole product + links + categories graph came back in ONE query
		assertThat(stats.getPrepareStatementCount()).isEqualTo(1L);
	}
}
