package com.interview.catalogservice.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.interview.catalogservice.service.ProductService;
import com.interview.catalogservice.web.Product;

@Controller
public class ProductGraphController {

	private final ProductService productService;
	
	public ProductGraphController(ProductService productService) {
		this.productService = productService;
	}
	
	@QueryMapping
	public Product product(@Argument Long id) {
		return productService.findById(id).orElse(null);
	}
	
	@QueryMapping
	public List<Product> products(@Argument String name) {
		return productService.search(name);
	}
}
