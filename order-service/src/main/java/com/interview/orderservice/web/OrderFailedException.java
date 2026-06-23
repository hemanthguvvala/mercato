package com.interview.orderservice.web;

public class OrderFailedException extends RuntimeException {

	public OrderFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
