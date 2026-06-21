package com.interview.orderservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.interview.orderservice.entity.AuditLogEntity;
import com.interview.orderservice.repository.AuditRepository;

@Service
public class AuditLogService {

	private final AuditRepository auditRepository;

	public AuditLogService(AuditRepository auditRepository) {
		this.auditRepository = auditRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void log(String action) {
		auditRepository.save(new AuditLogEntity(action));
	}

	public List<AuditLogEntity> getAllAuditLogs() {
		return auditRepository.findAll();
	}
}
