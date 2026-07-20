package com.interview.orderservice.web;

import java.time.LocalDateTime;

public record OrderSummary(Long orderId, String status, double total, int itemCount, LocalDateTime createdAt) {
}
