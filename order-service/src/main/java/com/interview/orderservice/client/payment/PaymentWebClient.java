package com.interview.orderservice.client.payment;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PaymentWebClient {

	private final WebClient webClient;
	
	public PaymentWebClient(WebClient.Builder builder) {
		this.webClient = builder.baseUrl("http://payment-service").build();
	}
	
	public void charge(ChargeRequest chargeRequest) {
		webClient.post()
		.uri("/payments/charge")
		.bodyValue(chargeRequest)
		.retrieve()
		.toBodilessEntity()
		.block();
	}
	
	public void refund(ChargeRequest chargeRequest) {
		webClient.post()
		.uri("/payments/refund")
		.bodyValue(chargeRequest)
		.retrieve()
		.toBodilessEntity()
		.block();
	}
	
}
