package com.interview.catalogservice.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.interview.catalogservice.service.ProductService;
import com.interview.catalogservice.web.Product;
import com.interview.catalogservice.web.ProductSearchCriteria;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/{id}")
	public ResponseEntity<Product> getProduct(@PathVariable("id") Long id) {
		return productService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping()
	public List<Product> search(@RequestParam(required = false) String name) {
		return productService.search(name);
	}
	
	@GetMapping("/search")
	public List<Product> searchProduct(@RequestParam(required = false) String name,
			@RequestParam(required = false) double minPrice, @RequestParam(required = false) double maxPrice,
			@RequestParam(required = false) Long categoryId) {
		ProductSearchCriteria criteria = new ProductSearchCriteria(name, minPrice, maxPrice, categoryId);
		return productService.search(criteria);
	}
	@PostMapping()
	public ResponseEntity<Product> create(@Valid @RequestBody Product productRequest) {
		Product saved = productService.create(productRequest);
		URI location = URI.create("/product/" + saved.id());
		return ResponseEntity.created(location).body(saved);

	}

	@PutMapping("/{id}")
	public ResponseEntity<Product> update(@PathVariable(name = "id") Long id, @RequestBody Product product) {
		return ResponseEntity.ok(productService.update(id, product));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/page")
	public Page<Product> page(Pageable pageable) {
		return productService.page(pageable);
	}

	@GetMapping("/slice")
	public Slice<Product> findBy(Pageable pageable) {
		return productService.findBy(pageable);
	}

}
