package com.interview.analyticsservice.event;

// This service's own copy of the OrderPlaced contract (same shape as the producer's).
public record OrderPlaced(Long orderId, String customerName, double totalAmount, int itemCount) {
}
