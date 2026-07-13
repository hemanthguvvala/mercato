package com.interview.orderservice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.orderservice.entity.OutboxEvent;
import com.interview.orderservice.event.OrderPlaced;
import com.interview.orderservice.repository.OutboxRepository;

@Component
public class OutboxPoller {

	private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

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
	public void publishPending() {
		List<OutboxEvent> events = outboxRepository.findByPublishedFalse();
		for (OutboxEvent event : events) {
			try {
				OrderPlaced orderPlaced = objectMapper.readValue(event.getPayload(), OrderPlaced.class);
				kafkaTemplate.send("order-events", String.valueOf(event.getAggregateId()), orderPlaced).get();
				event.markPublished();
				outboxRepository.save(event);
			} catch (Exception e) {
				log.error("Outbox event {} failed to publish; leaving unpublished for retry next poll", event.getId(),
						e);
			}
		}
	}
}
