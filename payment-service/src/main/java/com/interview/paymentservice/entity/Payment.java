package com.interview.paymentservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private Long orderId;

	private double amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	private LocalDateTime createdAt;

	protected Payment() {
	}

	public Payment(Long orderId, double amount, PaymentStatus status) {
		this.orderId = orderId;
		this.amount = amount;
		this.status = status;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getOrderId() {
		return orderId;
	}

	public double getAmount() {
		return amount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
