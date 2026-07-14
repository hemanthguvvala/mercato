package com.interview.analyticsservice.listener;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.interview.analyticsservice.event.OrderPlaced;

@Component
public class AnalyticsListener {


	private final StringRedisTemplate redisTemplate;

	public AnalyticsListener(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@KafkaListener(topics = "order-events")
	public void onOrderPlace(OrderPlaced orderPlacedEvent) {
		String key = "analytics:OrderPlaced:"+ orderPlacedEvent.orderId();
		if(Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			System.out.println("duplicate .. skipping.");
			return;
		}
		Long orders  =redisTemplate.opsForValue().increment("analytics:orders");
		Double revenue = redisTemplate.opsForValue().increment("analytics:revenue", orderPlacedEvent.totalAmount());
		System.out.println("📊 Analytics: " + orders + " orders, total revenue " + revenue);
		redisTemplate.opsForValue().setIfAbsent(key, "1",Duration.ofHours(24));
	}
}
