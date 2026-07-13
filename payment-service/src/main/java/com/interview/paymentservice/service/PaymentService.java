package com.interview.paymentservice.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.interview.paymentservice.web.PaymentDeclinedException;

@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	@Value("${payment.decline-above-amount}")
	private double declineAbove;
	private final Map<Long, Double> charged = new ConcurrentHashMap<>();
	private final Set<Long> refunded = ConcurrentHashMap.newKeySet();

	public void charge(Long orderId, double amount) {
		if (amount > declineAbove) {
			throw new PaymentDeclinedException(
					"Payment declined for order " + orderId + ": amount " + amount + " exceeds limit " + declineAbove);
		}
		if (charged.putIfAbsent(orderId, amount) != null) {
			log.info("Order {} already charged — skipping (idempotent)", orderId);
			return;
		}
		log.info("Charged order {} amount {}", orderId, amount);

	}

	public void refund(Long orderId) {
		Double chargedAmount = charged.get(orderId);
		if (chargedAmount == null) {
			log.warn("Refund requested for order {} that was never charged — ignoring", orderId);
			return;
		}
		if (!refunded.add(orderId)) {
			log.info("Order {} already refunded — skipping (idempotent)", orderId);
			return;
		}
		log.info("Refunded order {} amount {}", orderId, chargedAmount);

	}
	
	public Optional<Double> chargedAmount(Long orderId){
		return Optional.ofNullable(charged.get(orderId));
	}
	
	public boolean isRefunded(Long orderId) {
		return refunded.contains(orderId);
	}
}
