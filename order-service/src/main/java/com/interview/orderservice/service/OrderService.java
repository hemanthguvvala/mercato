package com.interview.orderservice.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.interview.orderservice.client.catalog.CatalogGateway;
import com.interview.orderservice.client.catalog.CatalogProduct;
import com.interview.orderservice.client.inventory.InventoryGateway;
import com.interview.orderservice.client.inventory.StockRequest;
import com.interview.orderservice.client.payment.ChargeRequest;
import com.interview.orderservice.client.payment.PaymentWebClient;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderItem;
import com.interview.orderservice.entity.OrderStatus;
import com.interview.orderservice.repository.OrderRepository;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;
import com.interview.orderservice.web.OrderFailedException;
import com.interview.orderservice.web.PaymentDeclinedException;
import com.interview.orderservice.web.PaymentUnavailableException;
import com.interview.orderservice.web.ResourceNotFoundException;

import feign.FeignException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final CatalogGateway catalogGateway;
	private final PaymentWebClient paymentWebClient;
	private final InventoryGateway inventoryGateway;
	private final OrderPersistence orderPersistence;
	private final Executor reservationExecutor;
	private final MeterRegistry meterRegistry;

	public OrderService(OrderRepository orderRepository, CatalogGateway catalogGateway,
			PaymentWebClient paymentWebClient, InventoryGateway inventoryGateway, OrderPersistence orderPersistence,
			@Qualifier("reservationExecutor") Executor executor, MeterRegistry meterRegistry) {
		this.catalogGateway = catalogGateway;
		this.orderRepository = orderRepository;
		this.paymentWebClient = paymentWebClient;
		this.inventoryGateway = inventoryGateway;
		this.orderPersistence = orderPersistence;
		this.reservationExecutor = executor;
		this.meterRegistry = meterRegistry;
	}

	public OrderResponse create(CreateOrderRequest request) {
		Timer.Sample sample = Timer.start(meterRegistry);
		boolean chargeAttempted = false;
		OrderEntity order = new OrderEntity(request.customerName());
		for (CreateOrderRequest.Line line : request.lines()) {
			CatalogProduct product = catalogGateway.getProduct(line.productId());
			order.addItem(new OrderItem(product.id(), product.name(), product.price(), line.quantity()));
		}
		OrderEntity savedOrder = orderPersistence.savePending(order);
		Long orderId = savedOrder.getId();
		double total = savedOrder.getItems().stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();

		List<StockRequest> requests = savedOrder.getItems().stream()
				.map(item -> new StockRequest(savedOrder.getId(), item.getProductId(), item.getQuantity())).toList();
		try {
			orderPersistence.transition(orderId, OrderStatus.RESERVING);
			CompletableFuture<?>[] futures = requests.stream()
					.map(req -> CompletableFuture.runAsync(() -> inventoryGateway.reserve(req), reservationExecutor))
					.toArray(CompletableFuture[]::new);
			CompletableFuture.allOf(futures).join();
			orderPersistence.transition(orderId, OrderStatus.STOCK_RESERVED);
			orderPersistence.transition(orderId, OrderStatus.AUTHORIZING);
			chargeAttempted = true;
			paymentWebClient.charge(new ChargeRequest(savedOrder.getId(), total));
			orderPersistence.transition(orderId, OrderStatus.PAYMENT_AUTHORIZED);
		} catch (CompletionException | FeignException | WebClientResponseException | WebClientRequestException
				| PaymentDeclinedException | PaymentUnavailableException ex) {
			orderPersistence.transition(orderId, OrderStatus.COMPENSATING);
			for (StockRequest r : requests) {
				try {
					inventoryGateway.release(r);
				} catch (Exception e) {
					log.error(
							"Compensation: failed to release stock for product {} on order {} — MANUAL RECOVERY NEEDED",
							r.productId(), savedOrder.getId(), e);
				}
			}
			if (chargeAttempted) {
				try {
					paymentWebClient.refund(new ChargeRequest(savedOrder.getId(), total));
				} catch (Exception e) {
					log.error("Compensation: refund failed for order {} — MANUAL RECOVERY NEEDED", savedOrder.getId(),
							e);
				}
			}
			orderPersistence.transition(orderId, OrderStatus.FAILED);
			sample.stop(meterRegistry.timer("orders.processing", "outcome", "failed"));
			meterRegistry.counter("orders.placed", "outcome", "failed").increment();
			Throwable cause = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
			throw new OrderFailedException("Order " + savedOrder.getId() + " failed: " + cause.getMessage(), cause);
		}
		try {
			orderPersistence.confirm(savedOrder.getId(), total);
		} catch (Exception e) {
			log.error("Order {} is paid (PAYMENT_AUTHORIZED) but confirm() failed — leaving it for the "
					+ "reconciler to finish forward; NOT compensating", orderId, e);
		}
		sample.stop(meterRegistry.timer("orders.processing", "outcome", "confirmed"));
		meterRegistry.counter("orders.placed", "outcome", "confirmed").increment();
		return toResponse(savedOrder);
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> findAll() {
		return orderRepository.findAllWithItems().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public OrderResponse findBy(Long id) {
		OrderEntity orderEntity = orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
		return toResponse(orderEntity);
	}

	@Transactional(readOnly = true)
	public int itemCountBad(Long orderId) {
		OrderEntity item = orderRepository.findById(orderId).orElseThrow();
		return item.getItems().size();
	}

	private OrderResponse toResponse(OrderEntity o) {
		List<OrderResponse.Item> items = o.getItems().stream().map(
				i -> new OrderResponse.Item(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity()))
				.toList();
		return new OrderResponse(o.getId(), o.getCustomerName(), items);
	}
}
