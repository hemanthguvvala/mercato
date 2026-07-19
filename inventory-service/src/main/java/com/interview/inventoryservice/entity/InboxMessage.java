package com.interview.inventoryservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inbox_message")
public class InboxMessage {

	@Id
	private String messageId;

	private LocalDateTime processedAt;

	protected InboxMessage() {
	}

	public InboxMessage(String messageId) {
		this.messageId = messageId;
		this.processedAt = LocalDateTime.now();
	}

	public String getMessageId() {
		return messageId;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}
}
