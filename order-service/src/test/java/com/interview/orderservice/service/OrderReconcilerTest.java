package com.interview.orderservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.interview.orderservice.client.inventory.InventoryClient;
import com.interview.orderservice.client.inventory.StockRequest;
import com.interview.orderservice.client.payment.ChargeRequest;
import com.interview.orderservice.client.payment.PaymentWebClient;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderItem;
import com.interview.orderservice.entity.OrderStatus;
import com.interview.orderservice.repository.OrderRepository;

/**
 * Unit tests for {@link OrderReconciler} — the durable-saga recovery job. Collaborators are mocked; no Spring/DB.
 *
 * Proves the point-of-no-return split: orders stuck BEFORE payment are compensated (backward),
 * orders stuck at {@code PAYMENT_AUTHORIZED} are driven forward to {@code confirm()} (never refunded),
 * a resumed {@code COMPENSATING} order is never re-transitioned, and one poison order never stops the batch.
 */
@ExtendWith(MockitoExtension.class)
class OrderReconcilerTest {

	@Mock
	OrderRepository orderRepository;
	@Mock
	OrderPersistence orderPersistence;
	@Mock
	InventoryClient inventoryClient;
	@Mock
	PaymentWebClient paymentWebClient;

	OrderReconciler reconciler;

	@BeforeEach
	void setUp() {
		reconciler = new OrderReconciler(orderRepository, orderPersistence, inventoryClient, paymentWebClient, 30L);
	}

	/** One line: product 1, price 499.0, qty 2 → total 998.0; id + status stamped directly. */
	private OrderEntity orderInStatus(Long id, OrderStatus status) {
		OrderEntity order = new OrderEntity("Hemanth");
		order.addItem(new OrderItem(1L, "Phone", 499.0, 2));
		ReflectionTestUtils.setField(order, "id", id);
		ReflectionTestUtils.setField(order, "status", status);
		return order;
	}

	@Test
	void pending_isCancelled_withNoSideEffects() {
		reconciler.recover(orderInStatus(100L, OrderStatus.PENDING));

		verify(orderPersistence).transition(100L, OrderStatus.CANCELLED);
		verify(inventoryClient, never()).release(any());
		verify(paymentWebClient, never()).refund(any());
		verify(orderPersistence, never()).confirm(anyLong(), anyDouble());
	}

	@Test
	void reserving_compensates_withoutRefund() {
		reconciler.recover(orderInStatus(100L, OrderStatus.RESERVING));

		verify(orderPersistence).transition(100L, OrderStatus.COMPENSATING);
		verify(inventoryClient).release(new StockRequest(100L, 1L, 2));
		verify(paymentWebClient, never()).refund(any()); // no charge happened yet
		verify(orderPersistence).transition(100L, OrderStatus.FAILED);
	}

	@Test
	void authorizing_compensates_withRefund_becauseChargeIsInDoubt() {
		reconciler.recover(orderInStatus(100L, OrderStatus.AUTHORIZING));

		verify(orderPersistence).transition(100L, OrderStatus.COMPENSATING);
		verify(inventoryClient).release(new StockRequest(100L, 1L, 2));
		verify(paymentWebClient).refund(new ChargeRequest(100L, 998.0));
		verify(orderPersistence).transition(100L, OrderStatus.FAILED);
	}

	@Test
	void compensating_resumes_withoutReTransitioning() {
		reconciler.recover(orderInStatus(100L, OrderStatus.COMPENSATING));

		verify(orderPersistence, never()).transition(100L, OrderStatus.COMPENSATING); // guard forbids C -> C
		verify(inventoryClient).release(new StockRequest(100L, 1L, 2));
		verify(paymentWebClient).refund(new ChargeRequest(100L, 998.0));
		verify(orderPersistence).transition(100L, OrderStatus.FAILED);
	}

	@Test
	void paymentAuthorized_isDrivenForward_toConfirm() {
		reconciler.recover(orderInStatus(100L, OrderStatus.PAYMENT_AUTHORIZED));

		verify(orderPersistence).confirm(100L, 998.0); // forward — never refund a paid customer
		verify(inventoryClient, never()).release(any());
		verify(paymentWebClient, never()).refund(any());
		verify(orderPersistence, never()).transition(anyLong(), any());
	}

	@Test
	void reconcile_processesEveryStaleOrder_evenWhenOneThrows() {
		OrderEntity poison = orderInStatus(1L, OrderStatus.PENDING);
		OrderEntity healthy = orderInStatus(2L, OrderStatus.PAYMENT_AUTHORIZED);
		when(orderRepository.findStaleForRecovery(any(), any())).thenReturn(List.of(poison, healthy));
		doThrow(new RuntimeException("db down")).when(orderPersistence).transition(1L, OrderStatus.CANCELLED);

		reconciler.reconcile();

		// the poison order blew up and was logged, but the healthy one was still finished forward
		verify(orderPersistence).confirm(2L, 998.0);
	}
}
