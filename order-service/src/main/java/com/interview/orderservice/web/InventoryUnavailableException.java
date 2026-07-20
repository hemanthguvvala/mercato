package com.interview.orderservice.web;

public class InventoryUnavailableException extends RuntimeException {

	public InventoryUnavailableException(Long productId, Throwable t) {
		super("Inventory unavailable for product " + productId, t);
	}

}
