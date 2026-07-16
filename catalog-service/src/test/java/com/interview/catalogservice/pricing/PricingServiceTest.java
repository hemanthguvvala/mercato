package com.interview.catalogservice.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Track G / G2 — Strategy pattern. Pure unit test (no Spring): construct the service with the
 * list of strategy beans exactly as Spring would inject them, and verify key-based selection,
 * each algorithm, and the unknown-key fallback to NONE.
 */
class PricingServiceTest {

	private final PricingService pricing = new PricingService(
			List.of(new NoDiscount(), new BulkDiscount(), new LoyaltyDiscount()));

	@Test
	void none_isPlainMultiplication() {
		assertThat(pricing.quote("NONE", 100.0, 2)).isEqualTo(200.0);
	}

	@Test
	void bulk_appliesTenPercentAtThreshold() {
		assertThat(pricing.quote("BULK", 100.0, 10)).isEqualTo(900.0); // 1000 - 10%
	}

	@Test
	void bulk_noDiscountBelowThreshold() {
		assertThat(pricing.quote("BULK", 100.0, 9)).isEqualTo(900.0); // 9 * 100, no discount
	}

	@Test
	void loyalty_appliesFlatFivePercent() {
		assertThat(pricing.quote("LOYALTY", 100.0, 1)).isEqualTo(95.0);
	}

	@Test
	void unknownKey_fallsBackToNone() {
		assertThat(pricing.quote("MYSTERY", 100.0, 2)).isEqualTo(200.0);
	}
}
