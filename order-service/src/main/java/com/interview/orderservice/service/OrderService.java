package com.interview.orderservice.service;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.orderservice.client.CatalogGateway;
import com.interview.orderservice.client.CatalogProduct;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderItem;
import com.interview.orderservice.event.OrderPlaced;
import com.interview.orderservice.repository.OrderRepository;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final CatalogGateway catalogGateway;
	private final KafkaTemplate<String, OrderPlaced> kafkaTemplate;

	public OrderService(OrderRepository orderRepository, CatalogGateway catalogGateway,
			KafkaTemplate<String, OrderPlaced> kafkaTemplate) {
		this.catalogGateway = catalogGateway;
		this.orderRepository = orderRepository;
		this.kafkaTemplate = kafkaTemplate;
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
		OrderPlaced event = new OrderPlaced(savedOrder.getId(), savedOrder.getCustomerName(), total,
				savedOrder.getItems().size());
		kafkaTemplate.send("order-events", event);
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
