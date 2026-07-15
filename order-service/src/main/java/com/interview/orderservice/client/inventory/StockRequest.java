package com.interview.orderservice.client.inventory;

public record StockRequest(Long orderId, Long productId, int quantity) {

}
