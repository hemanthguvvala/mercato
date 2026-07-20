package com.interview.orderservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_view")
public class OrderView {

	@Id
	private Long orderId;
	@Column(nullable = false)
	private String customerName;
	@Column(nullable = false)
	private String status;
	@Column(nullable = false)
	private double total;
	@Column(nullable = false)
	private int itemCount;
	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected OrderView() {
	}

	public OrderView(Long orderId, String customerName, String status, double total, int itemCount) {
		this.createdAt = LocalDateTime.now();
		this.customerName = customerName;
		this.status = status;
		this.total = total;
		this.orderId = orderId;
		this.itemCount = itemCount;
	}

	public Long getOrderId() {
		return orderId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public String getStatus() {
		return status;
	}

	public double getTotal() {
		return total;
	}

	public int getItemCount() {
		return itemCount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
