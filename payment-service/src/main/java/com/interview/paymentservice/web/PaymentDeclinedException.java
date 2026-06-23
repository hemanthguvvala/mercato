package com.interview.paymentservice.web;

public class PaymentDeclinedException extends RuntimeException{

	public PaymentDeclinedException(String message) {
		super(message);
	}
}
