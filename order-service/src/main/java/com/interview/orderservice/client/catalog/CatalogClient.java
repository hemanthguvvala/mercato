package com.interview.orderservice.client.catalog;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

	@GetMapping("/products/{id}")
	CatalogProduct getProduct(@PathVariable("id") Long id);
}
