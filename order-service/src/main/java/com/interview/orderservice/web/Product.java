package com.interview.orderservice.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record Product(Long id, @NotBlank(message = "Name is required") String name,
		@Positive(message = "price should be > 0") double price, Long version) {
}
