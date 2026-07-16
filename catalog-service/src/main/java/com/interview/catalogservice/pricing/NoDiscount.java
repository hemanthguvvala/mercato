package com.interview.catalogservice.pricing;

import org.springframework.stereotype.Component;

@Component
public class NoDiscount implements DiscountStrategy {

	@Override
	public String key() {
		return "NONE";
	}

	@Override
	public double totalFor(double unitPrice, int quantity) {
		return unitPrice * quantity;
	}

}
