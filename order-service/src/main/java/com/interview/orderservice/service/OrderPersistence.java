package com.interview.orderservice.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.events.OrderPlaced;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderStatus;
import com.interview.orderservice.entity.OutboxEvent;
import com.interview.orderservice.repository.OrderRepository;
import com.interview.orderservice.repository.OutboxRepository;

@Component
public class OrderPersistence {

	private final OrderRepository orderRepository;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper mapper;

	public OrderPersistence(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper mapper) {
		this.orderRepository = orderRepository;
		this.outboxRepository = outboxRepository;
		this.mapper = mapper;
	}

	@Transactional
	public OrderEntity savePending(OrderEntity order) {
		return orderRepository.save(order);
	}

	@Transactional
	public void confirm(Long orderId, double total) {
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalStateException("Order " + orderId + " vanished mid-saga"));
		order.transitionTo(OrderStatus.CONFIRMED);
		OrderPlaced event = new OrderPlaced(order.getId(), order.getCustomerName(), total, order.getItems().size());
		try {
			String payload = mapper.writeValueAsString(event);
			outboxRepository.save(new OutboxEvent(order.getId(), "OrderPlaced", payload));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize OrderPlaced", e);
		}

	}

	@Transactional
	public void transition(Long orderId, OrderStatus newStatus) {
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalStateException("Order " + orderId + " vanished mid-saga"));
		order.transitionTo(newStatus);
	}

}
