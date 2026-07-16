package com.interview.catalogservice.pricing;

import org.springframework.stereotype.Component;

@Component
public class BulkDiscount implements DiscountStrategy {

	@Override
	public String key() {
		return "BULK";
	}

	@Override
	public double totalFor(double unitPrice, int quantity) {
		double total = unitPrice * quantity;
		return quantity >= 10 ? total * 0.9 : total;
	}

}
