package com.interview.orderservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.orderservice.entity.OutboxEvent;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

	List<OutboxEvent> findByPublishedFalse();
}
