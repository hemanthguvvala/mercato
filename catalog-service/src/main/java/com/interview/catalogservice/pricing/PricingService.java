package com.interview.catalogservice.pricing;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class PricingService {

	private final Map<String, DiscountStrategy> strategies;

	public PricingService(List<DiscountStrategy> strategyList) {
		this.strategies = strategyList.stream().collect(Collectors.toMap(DiscountStrategy::key, Function.identity()));
	}

	public double quote(String strategyKey, double unitPrice, int quntity) {
		DiscountStrategy strategy = strategies.getOrDefault(strategyKey, strategies.get("NONE"));
		return strategy.totalFor(unitPrice, quntity);
	}
}
