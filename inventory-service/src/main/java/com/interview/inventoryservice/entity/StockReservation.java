package com.interview.inventoryservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "stock_reservation", uniqueConstraints = @UniqueConstraint(columnNames = { "order_id", "product_id" }))
public class StockReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long orderId;
	private Long productId;
	private int quantity;
	@Enumerated(EnumType.STRING)
	private ReservationStatus status;

	private LocalDateTime reservedAt;

	protected StockReservation() {
	}

	public StockReservation(Long orderId, Long productId, int quantity, ReservationStatus status) {
		this.orderId = orderId;
		this.productId = productId;
		this.quantity = quantity;
		this.status = status;
		this.reservedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getOrderId() {
		return orderId;
	}

	public Long getProductId() {
		return productId;
	}

	public int getQuantity() {
		return quantity;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public void setStatus(ReservationStatus status) {
		this.status = status;
	}

	public LocalDateTime getReservedAt() {
		return reservedAt;
	}
}
