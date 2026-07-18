package com.interview.orderservice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the order state machine — the {@link OrderStatus} transition rules and the
 * {@link OrderEntity#transitionTo(OrderStatus)} guard. Pure, no Spring/DB.
 */
class OrderStateMachineTest {

	@Test
	void newOrderStartsPending() {
		assertThat(new OrderEntity("Hemanth").getStatus()).isEqualTo(OrderStatus.PENDING);
	}

	@Test
	void walksTheFullHappyPath() {
		OrderEntity order = new OrderEntity("Hemanth");

		order.transitionTo(OrderStatus.RESERVING);
		order.transitionTo(OrderStatus.STOCK_RESERVED);
		order.transitionTo(OrderStatus.AUTHORIZING);
		order.transitionTo(OrderStatus.PAYMENT_AUTHORIZED);
		order.transitionTo(OrderStatus.CONFIRMED);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
	}

	@Test
	void rejectsIllegalTransition_andLeavesStatusUnchanged() {
		OrderEntity order = new OrderEntity("Hemanth"); // PENDING

		assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("PENDING -> CONFIRMED");

		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
	}

	@Test
	void everyFailureRoutesThroughCompensating_neverStraightToFailed() {
		assertThat(OrderStatus.RESERVING.canTransitionTo(OrderStatus.COMPENSATING)).isTrue();
		assertThat(OrderStatus.STOCK_RESERVED.canTransitionTo(OrderStatus.COMPENSATING)).isTrue();
		assertThat(OrderStatus.AUTHORIZING.canTransitionTo(OrderStatus.COMPENSATING)).isTrue();
		assertThat(OrderStatus.PAYMENT_AUTHORIZED.canTransitionTo(OrderStatus.COMPENSATING)).isTrue();
		assertThat(OrderStatus.COMPENSATING.canTransitionTo(OrderStatus.FAILED)).isTrue();

		// you cannot skip compensation and jump straight to FAILED
		assertThat(OrderStatus.RESERVING.canTransitionTo(OrderStatus.FAILED)).isFalse();
		assertThat(OrderStatus.PAYMENT_AUTHORIZED.canTransitionTo(OrderStatus.FAILED)).isFalse();
	}

	@Test
	void terminalStatesHaveNoExits() {
		assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
		assertThat(OrderStatus.FAILED.canTransitionTo(OrderStatus.PENDING)).isFalse();
		assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING)).isFalse();
	}
}
