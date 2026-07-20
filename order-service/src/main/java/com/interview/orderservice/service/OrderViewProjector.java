package com.interview.orderservice.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.events.OrderPlaced;
import com.interview.orderservice.entity.OrderView;
import com.interview.orderservice.repository.OrderViewRepository;

@Service
public class OrderViewProjector {

	private final OrderViewRepository orderViewRepository;

	public OrderViewProjector(OrderViewRepository orderViewRepository) {
		this.orderViewRepository = orderViewRepository;
	}

	@KafkaListener(groupId = "order-view", topics = "${app.kafka.order-events-topic}")
	@Transactional
	public void onOrderPlaced(OrderPlaced event) {
		OrderView view = orderViewRepository.findById(event.orderId()).orElseGet(() -> new OrderView(event.orderId(),
				event.customerName(), "CONFIRMED", event.totalAmount(), event.itemCount()));
		orderViewRepository.save(view);
	}
}
