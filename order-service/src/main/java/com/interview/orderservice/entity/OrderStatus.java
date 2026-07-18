package com.interview.orderservice.entity;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {

	PENDING, RESERVING, STOCK_RESERVED, AUTHORIZING, PAYMENT_AUTHORIZED, CONFIRMED, COMPENSATING, FAILED, CANCELLED;

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(PENDING, Set.of(RESERVING, CANCELLED),
			RESERVING, Set.of(STOCK_RESERVED, COMPENSATING), STOCK_RESERVED, Set.of(AUTHORIZING, COMPENSATING),
			AUTHORIZING, Set.of(PAYMENT_AUTHORIZED, COMPENSATING), PAYMENT_AUTHORIZED, Set.of(CONFIRMED, COMPENSATING),
			COMPENSATING, Set.of(FAILED), CONFIRMED, Set.of(), FAILED, Set.of(), CANCELLED, Set.of());

	public boolean canTransitionTo(OrderStatus next) {
		return ALLOWED.getOrDefault(this, Set.of()).contains(next);
	}
}
