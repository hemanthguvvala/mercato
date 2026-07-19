package com.interview.inventoryservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.interview.inventoryservice.entity.ReservationStatus;
import com.interview.inventoryservice.entity.StockReservation;
import com.interview.inventoryservice.repository.StockReservationRepository;

/**
 * Unit tests for {@link ReservationExpirySweep} — releases abandoned (unpaid) holds after a TTL.
 * Collaborators mocked. Proves it queries ONLY {@code RESERVED} rows (paid/CONFIRMED are excluded)
 * and that one failing release never stops the rest of the sweep.
 */
@ExtendWith(MockitoExtension.class)
class ReservationExpirySweepTest {

	@Mock
	StockReservationRepository reservationRepository;
	@Mock
	InventoryService inventoryService;

	ReservationExpirySweep sweep;

	@BeforeEach
	void setUp() {
		sweep = new ReservationExpirySweep(reservationRepository, inventoryService, 900L);
	}

	private StockReservation reservation(Long id, Long orderId, Long productId) {
		StockReservation r = new StockReservation(orderId, productId, 3, ReservationStatus.RESERVED);
		ReflectionTestUtils.setField(r, "id", id);
		return r;
	}

	@Test
	void queriesOnlyReservedRows_thenReleasesEach() {
		when(reservationRepository.findByStatusAndReservedAtBefore(eq(ReservationStatus.RESERVED), any()))
				.thenReturn(List.of(reservation(1L, 5L, 7L)));

		sweep.expireStaleReservations();

		verify(reservationRepository).findByStatusAndReservedAtBefore(eq(ReservationStatus.RESERVED),
				any(LocalDateTime.class));
		verify(inventoryService).release(5L, 7L);
	}

	@Test
	void oneFailingRelease_doesNotStopTheSweep() {
		when(reservationRepository.findByStatusAndReservedAtBefore(any(), any()))
				.thenReturn(List.of(reservation(1L, 5L, 7L), reservation(2L, 8L, 9L)));
		doThrow(new RuntimeException("row locked")).when(inventoryService).release(5L, 7L);

		sweep.expireStaleReservations();

		verify(inventoryService).release(5L, 7L); // threw
		verify(inventoryService).release(8L, 9L); // still processed
	}
}
