package com.interview.catalogservice.pricing;

import org.springframework.stereotype.Component;

@Component
public class LoyaltyDiscount implements DiscountStrategy {

	@Override
	public String key() {
		return "LOYALTY";
	}

	@Override
	public double totalFor(double unitPrice, int quantity) {
		return unitPrice * quantity * 0.95;
	}

}
