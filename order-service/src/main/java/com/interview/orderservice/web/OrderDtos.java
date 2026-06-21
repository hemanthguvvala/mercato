package com.interview.orderservice.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class OrderDtos {

	public record CreateOrderRequest(
			@NotBlank String customerName, 
			@NotEmpty List<@Valid Line> lines) {
		public record Line(
				@NotNull Long productId, 
				@Positive int quantity) {
		}
	}

	public record OrderResponse(Long id, String customerName, List<Item> items) {
		public record Item(Long productId, String productName, double unitPrice, int quantity) {
		}
	}
}
