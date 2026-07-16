package com.interview.orderservice.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.interview.orderservice.service.AuditLogService;

@Aspect
@Component
public class AuditAspect {

	private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

	private final AuditLogService auditLogService;

	public AuditAspect(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@AfterReturning(pointcut = "@annotation(audited)", returning = "result")
	public void audit(JoinPoint jp, Audited audited, Object result) {
		log.info("[AUDIT] action - '{}' method - {} result - {}", audited.action(), jp.getSignature().toShortString(),
				result);
		auditLogService.log(audited.action());
	}
}
