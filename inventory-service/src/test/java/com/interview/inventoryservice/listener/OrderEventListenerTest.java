package com.interview.inventoryservice.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.interview.events.OrderPlaced;
import com.interview.inventoryservice.entity.InboxMessage;
import com.interview.inventoryservice.entity.ReservationStatus;
import com.interview.inventoryservice.entity.StockReservation;
import com.interview.inventoryservice.repository.InboxRepository;
import com.interview.inventoryservice.repository.StockReservationRepository;

/**
 * Unit tests for {@link OrderEventListener} — the OrderPlaced inbox consumer, repositories mocked.
 *
 * Proves exactly-once processing via the inbox (a duplicate delivery is a no-op) and that only
 * {@code RESERVED} holds are flipped to {@code CONFIRMED} — a released one is left untouched, so the
 * Slice-2d expiry sweep (which only touches RESERVED) will now correctly leave paid orders alone.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

	@Mock
	InboxRepository inboxRepository;
	@Mock
	StockReservationRepository stockReservationRepository;

	OrderEventListener listener;

	@BeforeEach
	void setUp() {
		listener = new OrderEventListener(inboxRepository, stockReservationRepository);
	}

	@Test
	void firstDelivery_recordsInbox_andConfirmsOnlyReservedHolds() {
		StockReservation reserved = new StockReservation(100L, 1L, 2, ReservationStatus.RESERVED);
		StockReservation released = new StockReservation(100L, 2L, 1, ReservationStatus.RELEASED);
		when(stockReservationRepository.findByOrderId(100L)).thenReturn(List.of(reserved, released));

		listener.onOrderPlaced(new OrderPlaced(100L, "Hemanth", 998.0, 2));

		verify(inboxRepository).save(any(InboxMessage.class));                    // recorded in the inbox
		assertThat(reserved.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);  // RESERVED -> CONFIRMED
		assertThat(released.getStatus()).isEqualTo(ReservationStatus.RELEASED);   // left untouched
	}

	@Test
	void duplicateDelivery_isANoop() {
		when(inboxRepository.existsById("OrderPlaced:100")).thenReturn(true); // already processed

		listener.onOrderPlaced(new OrderPlaced(100L, "Hemanth", 998.0, 2));

		verify(inboxRepository, never()).save(any());                         // not recorded again
		verify(stockReservationRepository, never()).findByOrderId(anyLong()); // and no side effects
	}
}
