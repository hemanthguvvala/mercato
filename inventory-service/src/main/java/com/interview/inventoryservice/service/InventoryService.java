package com.interview.inventoryservice.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.inventoryservice.entity.InventoryItem;
import com.interview.inventoryservice.entity.ReservationStatus;
import com.interview.inventoryservice.entity.StockReservation;
import com.interview.inventoryservice.exception.InSufficientStockException;
import com.interview.inventoryservice.repository.InventoryRepository;
import com.interview.inventoryservice.repository.StockReservationRepository;

@Service
public class InventoryService {
	private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

	private final InventoryRepository inventoryRepository;
	private final StockReservationRepository reservationRepository;

	public InventoryService(InventoryRepository inventoryRepository, StockReservationRepository reservationRepository) {
		this.inventoryRepository = inventoryRepository;
		this.reservationRepository = reservationRepository;
	}

	@Transactional
	public void reserve(Long productId, int quantity) {
		InventoryItem item = inventoryRepository.findByProductId(productId)
				.orElseThrow(() -> new IllegalStateException("No invetory for prodcut " + productId));
		if (item.getAvailableQuantity() < quantity) {
			throw new InSufficientStockException("Insufficient stock for product " + productId + ": have "
					+ item.getAvailableQuantity() + ", need " + quantity);
		}

		item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
		inventoryRepository.save(item);
	}

	@Transactional
	public void reserve(Long orderId, Long productId, int quantity) {
		InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
				.orElseThrow(() -> new IllegalStateException("No inventory for product " + productId));

		if (reservationRepository.findByOrderIdAndProductId(orderId, productId).isPresent()) {
			log.debug("Reserve replay for order {} product {} — no-op", orderId, productId);
			return;
		}

		if (item.getAvailableQuantity() < quantity) {
			throw new InSufficientStockException("Insufficient stock for product " + productId + ": have "
					+ item.getAvailableQuantity() + ", need " + quantity);
		}

		item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
		inventoryRepository.save(item);
		reservationRepository.save(new StockReservation(orderId, productId, quantity, ReservationStatus.RESERVED));
	}

	@Transactional
	public void reservePessimistic(Long productId, int quantity) {
		InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
				.orElseThrow(() -> new IllegalStateException("No invetory for prodcut " + productId));
		if (item.getAvailableQuantity() < quantity) {
			throw new InSufficientStockException("Insufficient stock for product " + productId + ": have "
					+ item.getAvailableQuantity() + ", need " + quantity);
		}

		item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
		inventoryRepository.save(item);
	}

	@Transactional
	public void release(Long orderId, Long productId) {
		InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId).orElse(null);
		if (item == null) {
			log.debug("Release no-op: no inventory for product {}", productId);
			return;
		}

		Optional<StockReservation> maybe = reservationRepository.findByOrderIdAndProductId(orderId, productId);
		if (maybe.isEmpty() || maybe.get().getStatus() == ReservationStatus.RELEASED) {
			log.debug("Release no-op for order {} product {} (not reserved or already released)", orderId, productId);
			return;
		}

		StockReservation reservation = maybe.get();
		item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());
		inventoryRepository.save(item);
		reservation.setStatus(ReservationStatus.RELEASED);
		reservationRepository.save(reservation);
	}
}
