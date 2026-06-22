package com.interview.orderservice.event;


public record OrderPlaced(Long orderId, String customerName, double totalAmount, int itemCount) {

}
