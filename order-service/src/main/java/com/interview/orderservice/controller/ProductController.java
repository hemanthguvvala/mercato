package com.interview.orderservice.controller;

import com.interview.orderservice.service.OrderService;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.interview.orderservice.service.BusinessException;
import com.interview.orderservice.service.ProductBatchService;
import com.interview.orderservice.service.ProductService;
import com.interview.orderservice.web.Product;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/product")
public class ProductController {

	private final OrderService orderService;
	private final ProductService productService;
	private final ProductBatchService productBatchService;

	public ProductController(ProductService productService, ProductBatchService productBatchService, OrderService orderService) {
		this.productService = productService;
		this.productBatchService = productBatchService;
		this.orderService = orderService;
	}

	@GetMapping("/{id}")
	public ResponseEntity<Product> getProduct(@PathVariable("id") Long id) {
		return productService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping()
	public List<Product> search(@RequestParam(required = false) String name) {
		return productService.search(name);
	}

	@PostMapping("/checked")
	public ResponseEntity<String> createChecked(@RequestBody Product product) {
		try {
			productService.createThenFailChecked(product);
			return ResponseEntity.ok("created");
		} catch (BusinessException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}

	}

	@PostMapping()
	public ResponseEntity<Product> create(@Valid @RequestBody Product productRequest) {
		Product saved = productService.create(productRequest);
		URI location = URI.create("/product/" + saved.id());
		return ResponseEntity.created(location).body(saved);

	}
	
	@PutMapping("/{id}")
	public ResponseEntity<Product> update(@PathVariable(name = "id") Long id, @RequestBody Product product){
		return ResponseEntity.ok(productService.update(id, product));
	}


	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable("id") Long id){
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}
	
	@PostMapping("/batch")
	public ResponseEntity<List<Product>> createBatch(@RequestBody List<Product> products) {
		List<Product> prds = productBatchService.createBatch(products);
		return ResponseEntity.created(URI.create("/product/batch")).body(prds);
	}
	
	@GetMapping("/page")
	public Page<Product> page(Pageable pageable){
		return productService.page(pageable);
	}
	
	@GetMapping("/slice")
	public Slice<Product> findBy(Pageable pageable){
		return productService.findBy(pageable);
	}

}
