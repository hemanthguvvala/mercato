package com.interview.paymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.interview.paymentservice.web.PaymentDeclinedException;

class PaymentServiceTest {

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService();
		// @Value field isn't populated without Spring — set it directly (constructor injection would avoid this)
		ReflectionTestUtils.setField(paymentService, "declineAbove", 5000.0);
	}

	@Test
	void declinesChargeAboveLimit() {
		assertThatThrownBy(() -> paymentService.charge(1L, 6000.0))
				.isInstanceOf(PaymentDeclinedException.class);
		assertThat(paymentService.chargedAmount(1L)).isEmpty(); // declined -> nothing recorded
	}

	@Test
	void chargeAtLimitIsAccepted() {
		assertThatCode(() -> paymentService.charge(2L, 5000.0)).doesNotThrowAnyException();
		assertThat(paymentService.chargedAmount(2L)).contains(5000.0); // boundary: '>' is strict
	}

	@Test
	void chargeIsIdempotent() {
		paymentService.charge(3L, 100.0);
		paymentService.charge(3L, 999.0); // duplicate must be ignored
		assertThat(paymentService.chargedAmount(3L)).contains(100.0); // still the FIRST amount
	}

	@Test
	void refundOfNeverChargedOrderIsNoop() {
		paymentService.refund(42L);
		assertThat(paymentService.isRefunded(42L)).isFalse(); // never charged -> never refunded
	}

	@Test
	void refundIsRecordedAndIdempotent() {
		paymentService.charge(5L, 200.0);
		paymentService.refund(5L);
		assertThat(paymentService.isRefunded(5L)).isTrue();
		paymentService.refund(5L); // double refund -> still just once, no throw
		assertThat(paymentService.isRefunded(5L)).isTrue();
	}
}
