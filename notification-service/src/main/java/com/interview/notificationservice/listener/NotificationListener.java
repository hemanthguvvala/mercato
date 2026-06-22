package com.interview.notificationservice.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.interview.notificationservice.event.OrderPlaced;

@Component
public class NotificationListener {

	@KafkaListener(topics = "order-events")
	public void orderPlaced(OrderPlaced orderPlacedEvent) {
		System.out.println("📧 Order #" + orderPlacedEvent.orderId() + " for " + orderPlacedEvent.customerName()
				+ " — total " + orderPlacedEvent.totalAmount() + " (" + orderPlacedEvent.itemCount() + " items)");
	}
}
