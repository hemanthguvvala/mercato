package com.interview.inventoryservice.exception;

public class InSufficientStockException extends RuntimeException {

	public InSufficientStockException(String message) {
		super(message);
	}
}
