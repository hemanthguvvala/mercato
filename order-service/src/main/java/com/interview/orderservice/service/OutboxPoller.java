package com.interview.orderservice.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.orderservice.entity.OutboxEvent;
import com.interview.orderservice.event.OrderPlaced;
import com.interview.orderservice.repository.OutboxRepository;

@Component
public class OutboxPoller {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, OrderPlaced> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public OutboxPoller(OutboxRepository outboxRepository, KafkaTemplate<String, OrderPlaced> kafkaTemplate,
			ObjectMapper objectMapper) {
		this.outboxRepository = outboxRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
	}

	@Scheduled(fixedDelay = 2000)
	@Transactional
	public void publishPending() {
		List<OutboxEvent> events = outboxRepository.findByPublishedFalse();
		for (OutboxEvent event : events) {
			try {
				OrderPlaced orderPlaced = objectMapper.readValue(event.getPayload(), OrderPlaced.class);
				kafkaTemplate.send("order-events", orderPlaced).get();
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to deserialize OrderPlaced", e);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("Failed to Publish to kafka", e);
			}
			event.markPublished();
		}
	}
}
