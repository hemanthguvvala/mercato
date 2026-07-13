package com.interview.orderservice.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.orderservice.service.IdempotencyOrderService;
import com.interview.orderservice.service.OrderService;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final OrderService orderService;
	private final IdempotencyOrderService idempotencyOrderService;

	public OrderController(OrderService orderService, IdempotencyOrderService idempotencyOrderService) {
		this.orderService = orderService;
		this.idempotencyOrderService = idempotencyOrderService;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> create(
			@RequestHeader("Idempotency-Key") String idempotentkey,
			@Valid @RequestBody CreateOrderRequest request) {
		OrderResponse orderResponse = idempotencyOrderService.create(idempotentkey,request);
		return ResponseEntity.created(URI.create("/orders/" + orderResponse.id())).body(orderResponse);
	}

	@GetMapping
	public List<OrderResponse> getAll() {
		return orderService.findAll();
	}
	
	@GetMapping("/{id}/itemcount-bad")
	public int orderCount(@PathVariable("id") Long id) {
		return orderService.itemCountBad(id);
	}
}
