package com.interview.inventoryservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.interview.inventoryservice.entity.ReservationStatus;
import com.interview.inventoryservice.entity.StockReservation;
import com.interview.inventoryservice.repository.StockReservationRepository;

@Component
public class ReservationExpirySweep {

	private static final Logger log = LoggerFactory.getLogger(ReservationExpirySweep.class);

	private final StockReservationRepository stockReservationRepository;
	private final InventoryService inventoryService;
	private final long expireAfterSeconds;

	public ReservationExpirySweep(StockReservationRepository stockReservationRepository,
			InventoryService inventoryService,
			@Value("${inventory.reservation.expire-after-seconds}") long expireAfterSeconds) {
		this.expireAfterSeconds = expireAfterSeconds;
		this.inventoryService = inventoryService;
		this.stockReservationRepository = stockReservationRepository;
	}

	@Scheduled(fixedDelayString = "${inventory.reservation.sweep-delay-ms}")
	public void expireStaleReservations() {
		LocalDateTime cutoff = LocalDateTime.now().minusSeconds(expireAfterSeconds);
		List<StockReservation> stockReservations = stockReservationRepository
				.findByStatusAndReservedAtBefore(ReservationStatus.RESERVED, cutoff);
		for (StockReservation stockReservation : stockReservations) {
			try {
				inventoryService.release(stockReservation.getOrderId(), stockReservation.getProductId());
			} catch (Exception e) {
				log.error("failed to expire reservation {}", stockReservation.getId(), e);
			}
		}
	}
}
