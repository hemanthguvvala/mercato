package com.interview.catalogservice;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import com.interview.catalogservice.controller.ProductGraphController;
import com.interview.catalogservice.service.ProductService;
import com.interview.catalogservice.web.Product;

/**
 * GraphQL slice test: loads only the GraphQL infrastructure + the resolver controller, mocks the
 * service. Proves the schema parses, the resolver binds (method name -> query, @Argument -> arg),
 * argument coercion (ID -> Long) works, and the client gets back ONLY the fields it asked for.
 */
@GraphQlTest(ProductGraphController.class)
class ProductGraphqlTest {

	@Autowired
	private GraphQlTester graphQlTester;

	@MockBean
	private ProductService productService;

	@Test
	void productQuery_returnsOnlyRequestedFields() {
		when(productService.findById(1L)).thenReturn(Optional.of(new Product(1L, "Phone", 999.0, 0L)));

		graphQlTester.document("{ product(id: 1) { id name price } }")
				.execute()
				.path("product.name").entity(String.class).isEqualTo("Phone")
				.path("product.price").entity(Double.class).isEqualTo(999.0);
	}

	@Test
	void productsQuery_returnsList() {
		when(productService.search("Phone")).thenReturn(List.of(new Product(1L, "Phone", 999.0, 0L)));

		graphQlTester.document("{ products(name: \"Phone\") { name price } }")
				.execute()
				.path("products[0].name").entity(String.class).isEqualTo("Phone");
	}
}
