package com.interview.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares order-events with multiple partitions (R19) so consumer groups can process in
 * parallel — one active consumer thread per partition. Kafka only applies this when creating
 * the topic; an already-existing 1-partition topic must be repartitioned by an admin.
 */
@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic orderEventsTopic() {
		return TopicBuilder.name("order-events").partitions(3).replicas(1).build();
	}
}
