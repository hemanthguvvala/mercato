package com.interview.analyticsservice.listener;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.interview.analyticsservice.event.OrderPlaced;

@Component
public class AnalyticsListener {

	private double totalRevenue = 0;
	private int orderCount = 0;

	private final StringRedisTemplate redisTemplate;

	public AnalyticsListener(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@KafkaListener(topics = "order-events")
	public void onOrderPlace(OrderPlaced orderPlacedEvent) {
		String key = "analytics:OrderPlaced:"+ orderPlacedEvent.orderId();
		Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(key, "1",Duration.ofHours(24));
		if(Boolean.FALSE.equals(firstTime)) {
			System.out.println("duplicate .. skipping.");
			return;
		}
		orderCount++;
		totalRevenue += orderPlacedEvent.totalAmount();
		System.out.println("📊 Analytics: " + orderCount + " orders, total revenue " + totalRevenue);
	}
}
