package com.interview.orderservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.orderservice.entity.IdempotencyRecord;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
	Optional<IdempotencyRecord> findByIdempotentKey(String idempotentKey);
}
