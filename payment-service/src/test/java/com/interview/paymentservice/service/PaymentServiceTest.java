package com.interview.paymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.interview.paymentservice.entity.Payment;
import com.interview.paymentservice.entity.PaymentStatus;
import com.interview.paymentservice.repository.PaymentRepository;
import com.interview.paymentservice.web.PaymentDeclinedException;

/**
 * Unit tests for {@link PaymentService} — the durable-ledger logic, repository mocked.
 *
 * Proves: decline writes nothing, an existing row is not charged again (fast-path idempotency),
 * refund flips status via dirty-checking and is idempotent, and a refund for a never-charged order
 * is a no-op. The DB {@code unique(order_id)} constraint — the true exactly-once guard under a
 * concurrent double-charge — is enforced by Postgres and best proven by a @DataJpaTest (follow-up).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	PaymentRepository paymentRepository;

	PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(paymentRepository, 5000.0);
	}

	@Test
	void declinesChargeAboveLimit_andWritesNothing() {
		assertThatThrownBy(() -> paymentService.charge(1L, 6000.0))
				.isInstanceOf(PaymentDeclinedException.class);

		verify(paymentRepository, never()).save(any()); // declined -> no ledger row
	}

	@Test
	void chargeAtLimitIsAccepted_andPersistsAuthorized() {
		paymentService.charge(2L, 5000.0); // boundary: '>' is strict, so 5000 is accepted

		ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(captor.capture());
		Payment saved = captor.getValue();
		assertThat(saved.getOrderId()).isEqualTo(2L);
		assertThat(saved.getAmount()).isEqualTo(5000.0);
		assertThat(saved.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
	}

	@Test
	void chargeIsIdempotent_existingRowIsNotChargedAgain() {
		when(paymentRepository.existsByOrderId(3L)).thenReturn(true); // a row already exists

		paymentService.charge(3L, 999.0);

		verify(paymentRepository, never()).save(any()); // fast-path dedup — no second charge
	}

	@Test
	void refundOfNeverChargedOrderIsNoop() {
		// findByOrderId defaults to Optional.empty() -> never charged
		assertThatCode(() -> paymentService.refund(42L)).doesNotThrowAnyException();
		assertThat(paymentService.isRefunded(42L)).isFalse();
	}

	@Test
	void refund_flipsStatusToRefunded_andIsIdempotent() {
		Payment payment = new Payment(5L, 200.0, PaymentStatus.AUTHORIZED);
		when(paymentRepository.findByOrderId(5L)).thenReturn(Optional.of(payment));

		paymentService.refund(5L);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED); // dirty-checked transition

		paymentService.refund(5L); // second refund is a no-op, no throw
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		assertThat(paymentService.isRefunded(5L)).isTrue();
	}
}
