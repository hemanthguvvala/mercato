package com.interview.inventoryservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.inventoryservice.service.InventoryService;
import com.interview.inventoryservice.web.StockRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@PostMapping("/reserve")
	public void reserve(@Valid @RequestBody StockRequest request) {
		inventoryService.reserve(request.orderId(), request.productId(), request.quantity());
	}	 

	@PostMapping("/release")
	public void release(@Valid @RequestBody StockRequest request) {
		inventoryService.release(request.orderId(), request.productId());
	}
}
