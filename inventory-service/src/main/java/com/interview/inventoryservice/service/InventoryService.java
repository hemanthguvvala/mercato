package com.interview.inventoryservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.inventoryservice.entity.InventoryItem;
import com.interview.inventoryservice.exception.InSufficientStockException;
import com.interview.inventoryservice.repository.InventoryRepository;


@Service
public class InventoryService {

	private final InventoryRepository inventoryRepository;

	public InventoryService(InventoryRepository inventoryRepository) {
		this.inventoryRepository = inventoryRepository;
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
	public void release(Long productId, int quantity) {
		InventoryItem item = inventoryRepository.findByProductId(productId)
				.orElseThrow(() -> new IllegalStateException("No invetory for prodcut " + productId));
		item.setAvailableQuantity(item.getAvailableQuantity() + quantity);
		inventoryRepository.save(item);
	}
}
