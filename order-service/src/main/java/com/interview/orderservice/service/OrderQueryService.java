package com.interview.orderservice.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.interview.orderservice.entity.OrderView;
import com.interview.orderservice.repository.OrderViewRepository;
import com.interview.orderservice.web.OrderSummary;

@Service
public class OrderQueryService {

	private final OrderViewRepository orderViewRepository;

	public OrderQueryService(OrderViewRepository orderViewRepository) {
		this.orderViewRepository = orderViewRepository;
	}

	public Page<OrderSummary> findMyOrders(String customer, Pageable pageable) {
		return orderViewRepository.findByCustomerName(customer, pageable).map(this::toSummary);
	}

	public Optional<OrderSummary> findMine(Long orderId, String customer) {
		return orderViewRepository.findByOrderIdAndCustomerName(orderId, customer).map(this::toSummary);
	}

	private OrderSummary toSummary(OrderView orderView) {
		return new OrderSummary(orderView.getOrderId(), orderView.getStatus(), orderView.getTotal(),
				orderView.getItemCount(), orderView.getCreatedAt());
	}
}
