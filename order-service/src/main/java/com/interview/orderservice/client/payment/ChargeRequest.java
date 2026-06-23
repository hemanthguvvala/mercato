package com.interview.orderservice.client.payment;

public record ChargeRequest(Long orderId, double amount) {

}
