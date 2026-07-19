package com.interview.orderservice.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.interview.orderservice.client.inventory.InventoryClient;
import com.interview.orderservice.client.inventory.StockRequest;
import com.interview.orderservice.client.payment.ChargeRequest;
import com.interview.orderservice.client.payment.PaymentWebClient;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderStatus;
import com.interview.orderservice.repository.OrderRepository;

@Component
public class OrderReconciler {

	private static final Logger log = LoggerFactory.getLogger(OrderReconciler.class);

	private static final Set<OrderStatus> RECOVERABLE = EnumSet.of(OrderStatus.PENDING, OrderStatus.RESERVING,
			OrderStatus.STOCK_RESERVED, OrderStatus.AUTHORIZING, OrderStatus.PAYMENT_AUTHORIZED,
			OrderStatus.COMPENSATING);

	private final OrderRepository orderRepository;
	private final OrderPersistence orderPersistence;
	private final InventoryClient inventoryClient;
	private final PaymentWebClient paymentWebClient;

	private final long staleAfterSeconds;

	public OrderReconciler(OrderRepository orderRepository, OrderPersistence orderPersistence,
			InventoryClient inventoryClient, PaymentWebClient paymentWebClient,
			@Value("${order.reconciler.stale-after-seconds}") long staleAfterSeconds) {
		this.orderRepository = orderRepository;
		this.inventoryClient = inventoryClient;
		this.orderPersistence = orderPersistence;
		this.paymentWebClient = paymentWebClient;
		this.staleAfterSeconds = staleAfterSeconds;
	}

	@Scheduled(fixedDelayString = "${order.reconciler.fixed-delay-ms}")
	public void reconcile() {
		LocalDateTime cutoff = LocalDateTime.now().minusSeconds(staleAfterSeconds);
		List<OrderEntity> stale = orderRepository.findStaleForRecovery(RECOVERABLE, cutoff);
		for (OrderEntity order : stale) {
			try {
				recover(order);
			} catch (Exception e) {
				log.error("reconcile failed for order {} — will retry next run", order.getId(), e);
			}
		}
	}

	void recover(OrderEntity order) {
		Long orderId = order.getId();
		double total = order.getItems().stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();
		List<StockRequest> requests = order.getItems().stream()
				.map(item -> new StockRequest(order.getId(), item.getProductId(), item.getQuantity())).toList();
		log.warn("reconciler recovering order {} stuck in {}", orderId, order.getStatus());

		switch (order.getStatus()) {
			case PENDING -> orderPersistence.transition(orderId, OrderStatus.CANCELLED);
			case RESERVING, STOCK_RESERVED -> {
				orderPersistence.transition(orderId, OrderStatus.COMPENSATING);
				compensate(orderId, requests, total, false);
			}
			case AUTHORIZING -> {
				orderPersistence.transition(orderId, OrderStatus.COMPENSATING);
				compensate(orderId, requests, total, true);
			}
			case COMPENSATING -> compensate(orderId, requests, total, true);
			case PAYMENT_AUTHORIZED -> orderPersistence.confirm(orderId, total);
			default -> log.warn("reconciler: order {} in unexpected state {}", orderId, order.getStatus());
		}
	}

	private void compensate(Long orderId, List<StockRequest> requests, double total, boolean refund) {
		for (StockRequest request : requests) {
			inventoryClient.release(request);
		}
		if (refund) {
			paymentWebClient.refund(new ChargeRequest(orderId, total));
		}
		orderPersistence.transition(orderId, OrderStatus.FAILED);
	}
}
