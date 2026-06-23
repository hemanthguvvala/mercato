package com.interview.orderservice.client.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

	@PostMapping("/payments/charge")
	void charge(@RequestBody ChargeRequest chargeRequest);
	
	@PostMapping("/payments/refund")
	void refund(@RequestBody ChargeRequest chargeRequest);
}
