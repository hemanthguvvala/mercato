package com.interview.paymentservice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.paymentservice.service.PaymentService;
import com.interview.paymentservice.web.ChargeRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping("/charge")
	public void charge(@Valid @RequestBody ChargeRequest chargeRequest) {
		paymentService.charge(chargeRequest.orderId(), chargeRequest.amount());
	}

	@PostMapping("/refund")
	public void refund(@Valid @RequestBody ChargeRequest chargeRequest) {
		paymentService.refund(chargeRequest.orderId(), chargeRequest.amount());
	}
}
