package com.interview.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private String action;

	protected AuditLogEntity() {
	}

	public AuditLogEntity(String action) {
		this.action = action;
	}

	public Long getId() {
		return id;
	}

	public String getAction() {
		return action;
	}
}
