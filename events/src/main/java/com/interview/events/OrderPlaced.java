package com.interview.events;

public record OrderPlaced(Long orderId, String customerName, double totalAmount, int itemCount) {

}
