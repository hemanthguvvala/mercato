package com.interview.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.orderservice.entity.AuditLogEntity;

@Repository
public interface AuditRepository extends JpaRepository<AuditLogEntity, Long> {

}
