package com.interview.notificationservice.listener;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.interview.events.OrderPlaced;

@Component
public class NotificationListener {

	private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

	private final StringRedisTemplate redisTemplate;

	public NotificationListener(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@KafkaListener(topics = "order-events")
	public void orderPlaced(OrderPlaced orderPlacedEvent) {
		String key = "notification:OrderPlaced:" + orderPlacedEvent.orderId();
		if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			log.debug("Duplicate OrderPlaced {} — skipping", orderPlacedEvent.orderId());
			return;
		}
		log.info("Notify: order #{} for {} — total {} ({} items)", orderPlacedEvent.orderId(),
				orderPlacedEvent.customerName(), orderPlacedEvent.totalAmount(), orderPlacedEvent.itemCount());
		redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
	}
}
