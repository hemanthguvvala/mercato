package com.interview.analyticsservice.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.interview.analyticsservice.event.OrderPlaced;

@Component
public class AnalyticsListener {

	private double totalRevenue = 0;
	private int orderCount = 0;

	@KafkaListener(topics = "order-events")
	public void onOrderPlace(OrderPlaced orderPlacedEvent) {
		orderCount++;
		totalRevenue += orderPlacedEvent.totalAmount();
		System.out.println("📊 Analytics: " + orderCount + " orders, total revenue " + totalRevenue);
	}
}
