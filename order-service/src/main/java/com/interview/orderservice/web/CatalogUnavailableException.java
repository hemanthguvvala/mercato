package com.interview.orderservice.web;

public class CatalogUnavailableException extends RuntimeException {
	
	public CatalogUnavailableException(Long id, Throwable t) {
		 super("Catalog unavailable for product " + id, t);
	}

}
