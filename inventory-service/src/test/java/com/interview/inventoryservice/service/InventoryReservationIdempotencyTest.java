package com.interview.inventoryservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.interview.inventoryservice.entity.ReservationStatus;
import com.interview.inventoryservice.repository.InventoryRepository;
import com.interview.inventoryservice.repository.StockReservationRepository;

/**
 * R39 — reserve/release are idempotent and release is authoritative.
 *
 * @DataJpaTest gives a real H2 + JPA + transaction slice (no Docker, no Kafka, no Eureka);
 * @Import pulls the real InventoryService into the slice (it only needs the two repositories).
 * Flyway runs against the embedded H2 (V1 creates + seeds product 1 = 100; V2 creates the
 * reservation table), so this also proves our new migration and the entity mappings line up.
 *
 * Note: this proves the dedup LOGIC (a replay applies the delta at most once). The cross-request
 * concurrency/locking is a separate concern, exercised by InventoryConcurrencyTest.
 */
@DataJpaTest
@Import(InventoryService.class)
class InventoryReservationIdempotencyTest {

	@Autowired
	InventoryService inventoryService;
	@Autowired
	InventoryRepository inventoryRepository;
	@Autowired
	StockReservationRepository reservationRepository;

	// product 1 is seeded to 100 by Flyway V1__init.sql

	@Test
	void reserve_isIdempotent_perOrderAndProduct() {
		inventoryService.reserve(42L, 1L, 2);
		inventoryService.reserve(42L, 1L, 2); // replay of the SAME reservation

		// decremented exactly once (100 -> 98), and exactly one reservation row exists
		assertThat(inventoryRepository.findByProductId(1L).orElseThrow().getAvailableQuantity()).isEqualTo(98);
		assertThat(reservationRepository.count()).isEqualTo(1);
		assertThat(reservationRepository.findByOrderIdAndProductId(42L, 1L).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	void release_isIdempotent_andReturnsRecordedQuantity() {
		inventoryService.reserve(42L, 1L, 2); // 100 -> 98
		inventoryService.release(42L, 1L);    // 98  -> 100
		inventoryService.release(42L, 1L);    // replay: no-op, must NOT add stock again

		assertThat(inventoryRepository.findByProductId(1L).orElseThrow().getAvailableQuantity()).isEqualTo(100);
		assertThat(reservationRepository.findByOrderIdAndProductId(42L, 1L).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.RELEASED);
	}

	@Test
	void release_withNoMatchingReservation_isNoOp() {
		inventoryService.release(99L, 1L); // order 99 never reserved anything

		assertThat(inventoryRepository.findByProductId(1L).orElseThrow().getAvailableQuantity()).isEqualTo(100);
	}
}
