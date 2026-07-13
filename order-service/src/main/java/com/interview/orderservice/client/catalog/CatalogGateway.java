package com.interview.orderservice.client.catalog;

import org.springframework.stereotype.Component;

import com.interview.orderservice.web.CatalogUnavailableException;
import com.interview.orderservice.web.ResourceNotFoundException;

import feign.FeignException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class CatalogGateway {

	private final CatalogClient catalogClient;

	public CatalogGateway(CatalogClient catalogClient) {
		this.catalogClient = catalogClient;
	}

	@Retry(name = "catalog", fallbackMethod = "getProductFallback")
	@CircuitBreaker(name = "catalog")
	@RateLimiter(name = "catalog")
	@Bulkhead(name = "catalog")
	public CatalogProduct getProduct(Long id) {
		return catalogClient.getProduct(id);
	}

	public CatalogProduct getProductFallback(Long id, Throwable throwable) {
		if (throwable instanceof FeignException.NotFound) {
			throw new ResourceNotFoundException("Product Not Found : - " + id);
		}
		throw new CatalogUnavailableException(id, throwable);
	}
}
