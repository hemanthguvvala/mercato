package com.interview.orderservice.client.payment;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.interview.orderservice.web.PaymentDeclinedException;
import com.interview.orderservice.web.PaymentUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import reactor.core.publisher.Mono;

/**
 * Resilience boundary around the payment participant on the money path:
 * <ul>
 * <li>a response <b>timeout</b> so a slow payment-service can't hang the request thread;</li>
 * <li>a <b>circuit breaker</b> (name "payment") so repeated infra failures fail fast into compensation;</li>
 * <li>a 402 decline is converted to a distinct {@link PaymentDeclinedException} so the breaker treats it
 * as a business outcome (ignored) rather than an infra failure that would wrongly trip it.</li>
 * </ul>
 * No breaker on {@code refund} on purpose — compensation is best-effort and must not be fast-failed by an
 * open breaker (it only gets a timeout so it can't hang).
 */
@Component
public class PaymentWebClient {

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	private final WebClient webClient;

	public PaymentWebClient(WebClient.Builder builder) {
		this.webClient = builder.baseUrl("http://payment-service").build();
	}

	@CircuitBreaker(name = "payment", fallbackMethod = "chargeFallback")
	public void charge(ChargeRequest chargeRequest) {
		webClient.post()
				.uri("/payments/charge")
				.bodyValue(chargeRequest)
				.retrieve()
				.onStatus(status -> status.value() == 402,
						resp -> Mono.error(new PaymentDeclinedException(
								"Payment declined for order " + chargeRequest.orderId())))
				.toBodilessEntity()
				.timeout(TIMEOUT)
				.block();
	}

	public void refund(ChargeRequest chargeRequest) {
		webClient.post()
				.uri("/payments/refund")
				.bodyValue(chargeRequest)
				.retrieve()
				.toBodilessEntity()
				.timeout(TIMEOUT)
				.block();
	}

	@SuppressWarnings("unused")
	private void chargeFallback(ChargeRequest chargeRequest, Throwable t) {
		if (t instanceof PaymentDeclinedException declined) {
			throw declined; // business decline — not infra; the breaker ignores it (see config)
		}
		// timeout, breaker open, connection refused, 5xx, etc. → infra unavailable
		throw new PaymentUnavailableException(chargeRequest.orderId(), t);
	}
}
