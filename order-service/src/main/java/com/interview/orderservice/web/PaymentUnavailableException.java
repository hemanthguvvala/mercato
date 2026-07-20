package com.interview.orderservice.web;

public class PaymentUnavailableException extends RuntimeException {

	public PaymentUnavailableException(Long orderId, Throwable t) {
		super("Payment unavailable for order " + orderId, t);
	}

}
