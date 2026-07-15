package com.interview.orderservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.events.OrderPlaced;
import com.interview.orderservice.entity.OutboxEvent;
import com.interview.orderservice.repository.OutboxRepository;

/**
 * Proves the R5 fix: a single "poison" outbox event (one that fails to publish) must NOT block the
 * other events in the batch, and must NOT roll back events already published. The poison row simply
 * stays unpublished for the next poll.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

	@Mock
	OutboxRepository outboxRepository;
	@Mock
	KafkaTemplate<String, OrderPlaced> kafkaTemplate;
	@Mock
	ObjectMapper objectMapper;

	@InjectMocks
	OutboxPoller outboxPoller;

	@Test
	void poisonEventDoesNotBlockOrRollBackTheOthers() throws Exception {
		OutboxEvent good1 = new OutboxEvent(1L, "OrderPlaced", "good-1");
		OutboxEvent poison = new OutboxEvent(2L, "OrderPlaced", "poison"); // sits BETWEEN the good ones
		OutboxEvent good2 = new OutboxEvent(3L, "OrderPlaced", "good-3");
		when(outboxRepository.findByPublishedFalse()).thenReturn(List.of(good1, poison, good2));

		OrderPlaced parsed = new OrderPlaced(0L, "x", 0.0, 0);
		when(objectMapper.readValue("good-1", OrderPlaced.class)).thenReturn(parsed);
		when(objectMapper.readValue("good-3", OrderPlaced.class)).thenReturn(parsed);
		// the middle event fails — stands in for any publish-time failure (bad payload, Kafka error)
		when(objectMapper.readValue("poison", OrderPlaced.class)).thenThrow(new RuntimeException("corrupt payload"));

		CompletableFuture<SendResult<String, OrderPlaced>> ok = CompletableFuture.completedFuture(null);
		when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(ok);

		// must NOT throw, despite the poison event in the middle
		outboxPoller.publishPending();

		// both good events published AND marked (saved) — the one AFTER the poison still went out
		verify(kafkaTemplate).send("order-events", "1", parsed);
		verify(kafkaTemplate).send("order-events", "3", parsed);
		verify(outboxRepository).save(good1);
		verify(outboxRepository).save(good2);

		// the poison event was neither sent nor marked — it stays unpublished for retry (no HOL block)
		verify(kafkaTemplate, never()).send(eq("order-events"), eq("2"), any());
		verify(outboxRepository, never()).save(poison);
	}
}
