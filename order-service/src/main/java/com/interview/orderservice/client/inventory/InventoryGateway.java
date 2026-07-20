package com.interview.orderservice.client.inventory;

import org.springframework.stereotype.Component;

import com.interview.orderservice.web.InventoryUnavailableException;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Resilience boundary around the inventory participant on the money path.
 * Mirrors {@link com.interview.orderservice.client.catalog.CatalogGateway}: a
 * per-participant circuit breaker so a slow/dead inventory-service fails fast
 * (and the saga compensates) instead of hanging the request thread.
 *
 * No @Retry here on purpose — reserve is a write; retry is only added once its
 * idempotency (unique orderId+productId) is relied on. CB + timeout + the saga's
 * compensation already handle the failure correctly.
 */
@Component
public class InventoryGateway {

	private final InventoryClient inventoryClient;

	public InventoryGateway(InventoryClient inventoryClient) {
		this.inventoryClient = inventoryClient;
	}

	@CircuitBreaker(name = "inventory", fallbackMethod = "reserveFallback")
	public void reserve(StockRequest request) {
		inventoryClient.reserve(request);
	}

	public void release(StockRequest request) {
		inventoryClient.release(request);
	}

	@SuppressWarnings("unused")
	private void reserveFallback(StockRequest request, Throwable t) {
		if (t instanceof FeignException.NotFound notFound) {
			// Out-of-stock is a normal business rejection, not an infra failure. Let it
			// propagate as-is so the saga fails/compensates; the breaker ignores it (see config).
			throw notFound;
		}
		// Breaker open, timeout, connection refused, 5xx, etc. → infra unavailable.
		throw new InventoryUnavailableException(request.productId(), t);
	}
}
