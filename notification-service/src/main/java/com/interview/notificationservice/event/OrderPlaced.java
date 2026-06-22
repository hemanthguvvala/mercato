package com.interview.notificationservice.event;

// Same shape as order-service's OrderPlaced. Each service owns its own copy of the
// contract for now (a shared "events" module is a Phase 9 refinement). The consumer
// maps the incoming JSON onto THIS record by field name.
public record OrderPlaced(Long orderId, String customerName, double totalAmount, int itemCount) {
}
