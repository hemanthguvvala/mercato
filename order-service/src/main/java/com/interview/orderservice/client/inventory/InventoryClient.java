package com.interview.orderservice.client.inventory;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

	@PostMapping("/inventory/reserve")
	void reserve(@RequestBody StockRequest request);
	
	@PostMapping("/inventory/release")
	void release(@RequestBody StockRequest request);
}
