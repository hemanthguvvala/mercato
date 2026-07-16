package com.interview.analyticsservice.listener;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.interview.events.OrderPlaced;

@Component
public class AnalyticsListener {

	private static final Logger log = LoggerFactory.getLogger(AnalyticsListener.class);

	private final StringRedisTemplate redisTemplate;

	public AnalyticsListener(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@KafkaListener(topics = "${app.kafka.order-events-topic}")
	public void onOrderPlace(OrderPlaced orderPlacedEvent) {
		String key = "analytics:OrderPlaced:" + orderPlacedEvent.orderId();
		if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			log.debug("Duplicate OrderPlaced {} — skipping", orderPlacedEvent.orderId());
			return;
		}
		Long orders = redisTemplate.opsForValue().increment("analytics:orders");
		Double revenue = redisTemplate.opsForValue().increment("analytics:revenue", orderPlacedEvent.totalAmount());
		log.info("Analytics: {} orders, total revenue {}", orders, revenue);
		redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
	}
}
