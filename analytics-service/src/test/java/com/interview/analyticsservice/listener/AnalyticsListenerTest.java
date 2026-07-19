package com.interview.analyticsservice.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.interview.events.OrderPlaced;

/**
 * Unit tests for {@link AnalyticsListener} — the claim-before-effect fix (F21), Redis mocked.
 *
 * Proves the counters increment ONLY when the dedup claim (setIfAbsent) is won, and a duplicate
 * delivery (claim lost) increments nothing — i.e. the claim gates the effect atomically.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsListenerTest {

	@Mock
	StringRedisTemplate redisTemplate;
	@Mock
	ValueOperations<String, String> valueOps;

	AnalyticsListener listener;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		listener = new AnalyticsListener(redisTemplate);
	}

	@Test
	void claimWon_incrementsOrdersAndRevenue() {
		when(valueOps.setIfAbsent(eq("analytics:OrderPlaced:100"), eq("1"), any(Duration.class))).thenReturn(true);
		when(valueOps.increment("analytics:orders")).thenReturn(1L);
		when(valueOps.increment("analytics:revenue", 998.0)).thenReturn(998.0);

		listener.onOrderPlace(new OrderPlaced(100L, "Hemanth", 998.0, 2));

		verify(valueOps).increment("analytics:orders");
		verify(valueOps).increment("analytics:revenue", 998.0);
	}

	@Test
	void claimLost_duplicate_incrementsNothing() {
		when(valueOps.setIfAbsent(eq("analytics:OrderPlaced:100"), eq("1"), any(Duration.class))).thenReturn(false);

		listener.onOrderPlace(new OrderPlaced(100L, "Hemanth", 998.0, 2));

		verify(valueOps, never()).increment(anyString());
		verify(valueOps, never()).increment(anyString(), anyDouble());
	}
}
