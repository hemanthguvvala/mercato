package com.interview.orderservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox")
public class OutboxEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long aggregateId;
	private String eventType;
	@Lob
	private String payload;
	private boolean published;
	private LocalDateTime createdAt;

	protected OutboxEvent() {
	}

	public OutboxEvent(Long aggregateId, String eventType, String payload) {
		super();
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.published = false;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getEventType() {
		return eventType;
	}

	public String getPayload() {
		return payload;
	}
	
	public void markPublished() {
		this.published = true;
	}
}
