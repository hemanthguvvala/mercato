package com.interview.orderservice.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.interview.orderservice.entity.IdempotencyRecord;
import com.interview.orderservice.repository.IdempotencyRepository;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;

@Service
public class IdempotencyOrderService {

	private final IdempotencyRepository idempotencyRepository;
	private final OrderService orderService;

	public IdempotencyOrderService(IdempotencyRepository idempotencyRepository, OrderService orderService) {
		this.idempotencyRepository = idempotencyRepository;
		this.orderService = orderService;
	}

	public OrderResponse create(String key, CreateOrderRequest orderRequest) {
		Optional<IdempotencyRecord> existing = idempotencyRepository.findByIdempotentKey(key);
		if (existing.isPresent()) {
			return replay(existing.get());
		}
		IdempotencyRecord record;
		try {
			record = idempotencyRepository.saveAndFlush(new IdempotencyRecord(key));

		} catch (DataIntegrityViolationException raceLost) {
			return replay(idempotencyRepository.findByIdempotentKey(key).orElseThrow());
		}

		OrderResponse response = orderService.create(orderRequest);
		record.setOrderId(response.id());
		idempotencyRepository.save(record);
		return response;
	}

	private OrderResponse replay(IdempotencyRecord record) {
		if (record.getOrderId() == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Request with this Idempotency-Key is still processing");
		}
		return orderService.findBy(record.getOrderId());
	}
}
