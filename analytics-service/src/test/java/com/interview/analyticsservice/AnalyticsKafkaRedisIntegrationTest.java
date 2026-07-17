package com.interview.analyticsservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.interview.events.OrderPlaced;

/**
 * End-to-end proof (not mocks): an OrderPlaced published to a REAL Kafka broker is consumed by the
 * analytics listener and increments the counter in a REAL Redis — exercising the actual JSON
 * deserialization config (use.type.headers=false, default.type, trusted.packages) that this service
 * relies on. Runs only in CI (@Tag("integration")); the GitHub runner provides the Docker daemon
 * Testcontainers needs.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AnalyticsKafkaRedisIntegrationTest {

	@Container
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

	@Container
	static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void overrideInfra(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
		registry.add("spring.data.redis.password", () -> "");
		registry.add("spring.data.redis.ssl.enabled", () -> "false");
	}

	@Autowired
	KafkaTemplate<String, OrderPlaced> kafkaTemplate;

	@Autowired
	StringRedisTemplate redisTemplate;

	@Test
	void consumesOrderPlacedFromKafkaAndIncrementsAnalyticsCounter() {
		kafkaTemplate.send("order-events", "1", new OrderPlaced(1L, "alice", 250.0, 3));

		// the listener consumes asynchronously; poll Redis until the counter reflects the one event
		await().atMost(30, TimeUnit.SECONDS).untilAsserted(
				() -> assertThat(redisTemplate.opsForValue().get("analytics:orders")).isEqualTo("1"));
	}
}
