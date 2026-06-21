package com.interview.orderservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.orderservice.entity.AuditLogEntity;
import com.interview.orderservice.service.AuditLogService;

@RestController
@RequestMapping("/audit")
public class AuditController {

	private final AuditLogService auditLogService;

	public AuditController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	public List<AuditLogEntity> getAll() {
		return auditLogService.getAllAuditLogs();
	}
}
