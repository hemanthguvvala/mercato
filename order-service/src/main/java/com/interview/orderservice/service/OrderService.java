package com.interview.orderservice.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.orderservice.client.catalog.CatalogGateway;
import com.interview.orderservice.client.catalog.CatalogProduct;
import com.interview.orderservice.client.inventory.InventoryClient;
import com.interview.orderservice.client.inventory.StockRequest;
import com.interview.orderservice.client.payment.ChargeRequest;
import com.interview.orderservice.client.payment.PaymentWebClient;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderItem;
import com.interview.orderservice.entity.OutboxEvent;
import com.interview.orderservice.event.OrderPlaced;
import com.interview.orderservice.repository.OrderRepository;
import com.interview.orderservice.repository.OutboxRepository;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;
import com.interview.orderservice.web.OrderFailedException;

import feign.FeignException;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final CatalogGateway catalogGateway;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final PaymentWebClient paymentWebClient;
	private final InventoryClient inventoryClient;

	public OrderService(OrderRepository orderRepository, CatalogGateway catalogGateway,
			OutboxRepository outboxRepository, ObjectMapper objectMapper,
			PaymentWebClient paymentWebClient, InventoryClient inventoryGateWay) {
		this.catalogGateway = catalogGateway;
		this.orderRepository = orderRepository;
		this.outboxRepository = outboxRepository;
		this.paymentWebClient = paymentWebClient;
		this.objectMapper = objectMapper;
		this.inventoryClient = inventoryGateWay;
	}

	@Transactional
	public OrderResponse create(CreateOrderRequest request) {
		OrderEntity order = new OrderEntity(request.customerName());
		for (CreateOrderRequest.Line line : request.lines()) {
			CatalogProduct product = catalogGateway.getProduct(line.productId());
			order.addItem(new OrderItem(product.id(), product.name(), product.price(), line.quantity()));
		}
		OrderEntity savedOrder = orderRepository.save(order);
		double total = savedOrder.getItems().stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();

		List<StockRequest> reserved = new ArrayList<>();

		try {
			for (OrderItem item : savedOrder.getItems()) {
				StockRequest req = new StockRequest(item.getProductId(), item.getQuantity());
				inventoryClient.reserve(req);
				reserved.add(req);
			}
			paymentWebClient.charge(new ChargeRequest(savedOrder.getId(), total));
		} catch (FeignException | WebClientResponseException ex) {

			for (StockRequest r : reserved) {
				inventoryClient.release(r);
			}

			throw new OrderFailedException("Order " + savedOrder.getId() + " failed: " + ex.getMessage(), ex);
		}

		OrderPlaced event = new OrderPlaced(savedOrder.getId(), savedOrder.getCustomerName(), total,
				savedOrder.getItems().size());
		try {
			String payload = objectMapper.writeValueAsString(event);
			outboxRepository.save(new OutboxEvent(savedOrder.getId(), "OrderPlaced", payload));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize OrderPlaced",e);
		}
		
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
