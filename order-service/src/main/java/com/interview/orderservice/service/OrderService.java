package com.interview.orderservice.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.interview.orderservice.client.catalog.CatalogGateway;
import com.interview.orderservice.client.catalog.CatalogProduct;
import com.interview.orderservice.client.inventory.InventoryClient;
import com.interview.orderservice.client.inventory.StockRequest;
import com.interview.orderservice.client.payment.ChargeRequest;
import com.interview.orderservice.client.payment.PaymentWebClient;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderItem;
import com.interview.orderservice.repository.OrderRepository;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;
import com.interview.orderservice.web.OrderFailedException;

import feign.FeignException;

@Service
public class OrderService {
	
	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final CatalogGateway catalogGateway;
	private final PaymentWebClient paymentWebClient;
	private final InventoryClient inventoryClient;
	private final OrderPersistence orderPersistence;

	public OrderService(OrderRepository orderRepository, CatalogGateway catalogGateway,
			PaymentWebClient paymentWebClient, InventoryClient inventoryGateWay,
			OrderPersistence orderPersistence) {
		this.catalogGateway = catalogGateway;
		this.orderRepository = orderRepository;
		this.paymentWebClient = paymentWebClient;
		this.inventoryClient = inventoryGateWay;
		this.orderPersistence = orderPersistence;
	}

	public OrderResponse create(CreateOrderRequest request) {
		boolean chargeAttempted  = false;
		OrderEntity order = new OrderEntity(request.customerName());
		for (CreateOrderRequest.Line line : request.lines()) {
			CatalogProduct product = catalogGateway.getProduct(line.productId());
			order.addItem(new OrderItem(product.id(), product.name(), product.price(), line.quantity()));
		}
		OrderEntity savedOrder = orderPersistence.savePending(order);
		double total = savedOrder.getItems().stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();

		List<StockRequest> reserved = new ArrayList<>();

		try {
			for (OrderItem item : savedOrder.getItems()) {
				StockRequest req = new StockRequest(item.getProductId(), item.getQuantity());
				inventoryClient.reserve(req);
				reserved.add(req);
			}
			chargeAttempted = true;
			paymentWebClient.charge(new ChargeRequest(savedOrder.getId(), total));
		} catch (FeignException | WebClientResponseException | WebClientRequestException ex) {
			for (StockRequest r : reserved) {
				try {
					inventoryClient.release(r);
				} catch (Exception e) {
					log.error(
							"Compensation: failed to release stock for product {} on order {} — MANUAL RECOVERY NEEDED",
							r.productId(), savedOrder.getId(), e);
				}
			}
			orderPersistence.fail(savedOrder.getId());
			if (chargeAttempted) {
				try {
					paymentWebClient.refund(new ChargeRequest(savedOrder.getId(), total));
				} catch (Exception e) {
					log.error("Compensation: refund failed for order {} — MANUAL RECOVERY NEEDED", savedOrder.getId(),
							e);
				}

			}
			throw new OrderFailedException("Order " + savedOrder.getId() + " failed: " + ex.getMessage(), ex);
		}
		
		orderPersistence.confirm(savedOrder.getId(), total);
		
		return toResponse(savedOrder);
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> findAll() {
		return orderRepository.findAllWithItems().stream().map(this::toResponse).toList();
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
