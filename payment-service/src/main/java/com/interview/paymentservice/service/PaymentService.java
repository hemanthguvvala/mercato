package com.interview.paymentservice.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.paymentservice.entity.Payment;
import com.interview.paymentservice.entity.PaymentStatus;
import com.interview.paymentservice.repository.PaymentRepository;
import com.interview.paymentservice.web.PaymentDeclinedException;

@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final PaymentRepository paymentRepository;
	private final double declineAbove;

	public PaymentService(PaymentRepository paymentRepository,
			@Value("${payment.decline-above-amount}") double declineAbove) {
		this.paymentRepository = paymentRepository;
		this.declineAbove = declineAbove;
	}

	@Transactional
	public void charge(Long orderId, double amount) {
		if (amount > declineAbove) {
			throw new PaymentDeclinedException(
					"Payment declined for order " + orderId + ": amount " + amount + " exceeds limit " + declineAbove);
		}
		if (paymentRepository.existsByOrderId(orderId)) {
			log.info("Order {} already charged — skipping (idempotent)", orderId);
			return;
		}
		paymentRepository.save(new Payment(orderId, amount, PaymentStatus.AUTHORIZED));
		log.info("Charged order {} amount {}", orderId, amount);

	}

	@Transactional
	public void refund(Long orderId) {
		Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
		if (payment == null) {
			log.warn("Refund requested for order {} that was never charged — ignoring", orderId);
			return;
		}
		if (payment.getStatus() == PaymentStatus.REFUNDED) {
			log.info("Order {} already refunded — skipping (idempotent)", orderId);
			return;
		}
		payment.setStatus(PaymentStatus.REFUNDED);
		log.info("Refunded order {} amount {}", orderId, payment.getAmount());

	}

	@Transactional(readOnly = true)
	public Optional<Double> chargedAmount(Long orderId) {
		return paymentRepository.findByOrderId(orderId).map(Payment::getAmount);
	}

	@Transactional(readOnly = true)
	public boolean isRefunded(Long orderId) {
		return paymentRepository.findByOrderId(orderId).map(p -> p.getStatus() == PaymentStatus.REFUNDED).orElse(false);
	}
}
