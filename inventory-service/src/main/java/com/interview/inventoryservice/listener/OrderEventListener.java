package com.interview.inventoryservice.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.interview.events.OrderPlaced;
import com.interview.inventoryservice.entity.InboxMessage;
import com.interview.inventoryservice.entity.ReservationStatus;
import com.interview.inventoryservice.entity.StockReservation;
import com.interview.inventoryservice.repository.InboxRepository;
import com.interview.inventoryservice.repository.StockReservationRepository;

@Component
public class OrderEventListener {

	private final InboxRepository inboxRepository;
	private final StockReservationRepository stockReservationRepository;

	private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

	public OrderEventListener(InboxRepository inboxRepository, StockReservationRepository stockReservationRepository) {
		this.inboxRepository = inboxRepository;
		this.stockReservationRepository = stockReservationRepository;
	}

	@KafkaListener(topics = "${app.kafka.order-events-topic}")
	@Transactional
	public void onOrderPlaced(OrderPlaced event) {
		String messageId = "OrderPlaced:" + event.orderId();
		if (inboxRepository.existsById(messageId)) {
			log.info("duplicate , skipping");
			return;
		}
		inboxRepository.save(new InboxMessage(messageId));
		List<StockReservation> stockReservations = stockReservationRepository.findByOrderId(event.orderId());
		for (StockReservation stockReservation : stockReservations) {
			if (stockReservation.getStatus() == ReservationStatus.RESERVED) {
				stockReservation.setStatus(ReservationStatus.CONFIRMED);
			}
		}
	}
}
