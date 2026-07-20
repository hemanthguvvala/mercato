package com.interview.orderservice.web;

public class PaymentDeclinedException extends RuntimeException {

	public PaymentDeclinedException(String message) {
		super(message);
	}

}
