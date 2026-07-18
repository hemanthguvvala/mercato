package com.interview.orderservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private OrderEntity order;
	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	private OrderStatus fromStatus;
	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false)
	private OrderStatus toStatus;
	@Column(nullable = false)
	private LocalDateTime changedAt;

	protected OrderStatusHistory() {
	}

	public OrderStatusHistory(OrderEntity order, OrderStatus fromStatus, OrderStatus toStatus) {
		this.order = order;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.changedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public OrderStatus getFromStatus() {
		return fromStatus;
	}

	public OrderStatus getToStatus() {
		return toStatus;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}
}
