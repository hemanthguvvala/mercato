package com.interview.inventoryservice.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockRequest(@NotNull Long orderId, @NotNull Long productId, @Positive int quantity) {

}
