package com.interview.catalogservice.pricing;

public interface DiscountStrategy {

	String key();

	double totalFor(double unitPrice, int quantity);
}
