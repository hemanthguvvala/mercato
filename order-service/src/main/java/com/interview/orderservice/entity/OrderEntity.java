package com.interview.orderservice.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String customerName;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderStatusHistory> statusHistory = new ArrayList<>();

	protected OrderEntity() {
	}

	public OrderEntity(String customerName) {
		this.customerName = customerName;
		this.status = OrderStatus.PENDING;
	}

	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
	}

	public Long getId() {
		return id;
	};

	public String getCustomerName() {
		return customerName;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public OrderStatus getStatus() {
		return this.status;
	}

	public void transitionTo(OrderStatus next) {
		if (!status.canTransitionTo(next))
			throw new IllegalStateException("Illegal order transition: " + status + " -> " + next);
		statusHistory.add(new OrderStatusHistory(this, this.status, next));
		this.status = next;
	}
}
