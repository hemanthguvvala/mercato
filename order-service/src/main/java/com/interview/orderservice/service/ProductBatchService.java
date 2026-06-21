package com.interview.orderservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.orderservice.web.Product;

@Service
public class ProductBatchService {

	public final ProductService productService;

	public ProductBatchService(ProductService productService) {
		this.productService = productService;
	}

	@Transactional
	public List<Product> createBatch(List<Product> products) {
		return products.stream().map(i -> productService.create(i)).toList();
	}
}
