package com.interview.notificationservice.listener;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.interview.notificationservice.event.OrderPlaced;

@Component
public class NotificationListener {
	
	private final StringRedisTemplate redisTemplate;
	
	public NotificationListener(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@KafkaListener(topics = "order-events")
	public void orderPlaced(OrderPlaced orderPlacedEvent) {
		String key = "notification:OrderPlaced:" + orderPlacedEvent.orderId();
		Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
		if (Boolean.FALSE.equals(firstTime)) {
			System.out.println("duplicate..skipping");
			return;
		}
		System.out.println("📧 Order #" + orderPlacedEvent.orderId() + " for " + orderPlacedEvent.customerName()
				+ " — total " + orderPlacedEvent.totalAmount() + " (" + orderPlacedEvent.itemCount() + " items)");
	}
}
