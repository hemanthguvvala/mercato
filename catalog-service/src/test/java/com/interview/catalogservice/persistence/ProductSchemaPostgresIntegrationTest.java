package com.interview.catalogservice.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.interview.catalogservice.entities.Category;
import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.CategoryRepository;
import com.interview.catalogservice.repository.ProductRepository;
import com.interview.catalogservice.service.ProductService;

/**
 * Proves the Flyway migrations (V1-V6) and the JPA mappings actually run on REAL Postgres, not just
 * H2 — closing the "H2 file DB isn't production" gap. This is the lens that surfaced the H2-isms
 * (`double`, `clob`) that Postgres rejects.
 *
 * @DataJpaTest slice + a Postgres Testcontainer; replace=NONE so it uses the container (not an
 * embedded DB), Flyway builds the schema on it, Hibernate validates against it. Runs only in CI
 * (@Tag("integration")) where the runner provides Docker.
 */
@Tag("integration")
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(ProductService.class)
class ProductSchemaPostgresIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // schema comes from Flyway, Hibernate only checks it
	}

	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	TestEntityManager em;

	@Test
	void migrationsApplyAndManyToManyRoundTripsOnPostgres() {
		ProductEntity phone = productRepository.save(new ProductEntity("Phone", 499.0));
		Category electronics = categoryRepository.save(new Category("Electronics"));

		productService.assignCategory(phone.getId(), electronics.getId(), 0);
		em.flush();
		em.clear();

		ProductEntity reloaded = productRepository.findById(phone.getId()).orElseThrow();
		assertThat(reloaded.getPrice()).isEqualTo(499.0); // the V1 'double precision' column
		assertThat(reloaded.getCategories()).extracting(pc -> pc.getCategory().getName())
				.containsExactly("Electronics"); // V2 category + V3 product_category link
	}
}
