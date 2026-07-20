package com.interview.orderservice.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.orderservice.service.IdempotencyOrderService;
import com.interview.orderservice.service.OrderQueryService;
import com.interview.orderservice.service.OrderService;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;
import com.interview.orderservice.web.OrderSummary;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final OrderService orderService;
	private final IdempotencyOrderService idempotencyOrderService;
	private final OrderQueryService orderQueryService;

	public OrderController(OrderService orderService, IdempotencyOrderService idempotencyOrderService,
			OrderQueryService orderQueryService) {
		this.orderService = orderService;
		this.idempotencyOrderService = idempotencyOrderService;
		this.orderQueryService = orderQueryService;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> create(@RequestHeader("Idempotency-Key") String idempotentkey,
			@Valid @RequestBody CreateOrderRequest request) {
		OrderResponse orderResponse = idempotencyOrderService.create(idempotentkey, request);
		return ResponseEntity.created(URI.create("/orders/" + orderResponse.id())).body(orderResponse);
	}

	@GetMapping
	public Page<OrderSummary> myOrders(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
		return orderQueryService.findMyOrders(jwt.getSubject(), pageable);
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderSummary> one(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		return orderQueryService.findMine(id, jwt.getSubject()).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/{id}/itemcount-bad")
	public int orderCount(@PathVariable("id") Long id) {
		return orderService.itemCountBad(id);
	}
}
