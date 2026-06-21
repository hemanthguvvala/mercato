package com.interview.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service", url = "${catalog.service.url}")
public interface CatalogClient {

	@GetMapping("/products/{id}")
	CatalogProduct getProduct(@PathVariable("id") Long id);
}
