package com.interview.paymentservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.interview.paymentservice.web.PaymentDeclinedException;

@Service
public class PaymentService {

	@Value("${payment.decline-above-amount}")
	private double declineAbove;

	public void charge(Long orderId, double amount) {
		if (amount > declineAbove) {
			throw new PaymentDeclinedException(
					"Payment declined for order " + orderId + ": amount " + amount + " exceeds limit " + declineAbove);
		}
		System.out.println("💳 Charged order " + orderId + " amount " + amount);
	}

	public void refund(Long orderId, double amount) {
		System.out.println("↩️ Refunded order " + orderId + " amount " + amount);
	}
}
