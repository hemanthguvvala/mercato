package com.interview.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.interview.apigateway.ratelimit.RateLimitFilter;

import reactor.core.publisher.Mono;

/**
 * The point of this filter is fail-CLOSED behaviour: Spring Cloud Gateway's built-in RedisRateLimiter
 * silently ALLOWS traffic when Redis is unreachable (fail open). {@link #redisDown_failsClosedWith503}
 * is the regression guard for that bug — a Redis error must yield 503, not a bypassed limiter.
 */
class RateLimitFilterTest {

	private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
	@SuppressWarnings("unchecked")
	private final ReactiveValueOperations<String, String> valueOps = mock(ReactiveValueOperations.class);
	private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

	private final RateLimitFilter filter = new RateLimitFilter(redis, 10, 1);

	@Test
	void underLimit_passesThrough() {
		when(redis.opsForValue()).thenReturn(valueOps);
		when(valueOps.increment(anyString())).thenReturn(Mono.just(1L));
		when(redis.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
		when(chain.filter(any())).thenReturn(Mono.empty());

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/products/1"));
		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
		assertThat(exchange.getResponse().getStatusCode()).isNull();
	}

	@Test
	void overLimit_returns429() {
		when(redis.opsForValue()).thenReturn(valueOps);
		when(valueOps.increment(anyString())).thenReturn(Mono.just(11L));

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/products/1"));
		filter.filter(exchange, chain).block();

		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	void redisDown_failsClosedWith503() {
		when(redis.opsForValue()).thenReturn(valueOps);
		when(valueOps.increment(anyString())).thenReturn(Mono.error(new RuntimeException("redis down")));

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/products/1"));
		filter.filter(exchange, chain).block();

		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void nonProductsPath_isNotRateLimited() {
		when(chain.filter(any())).thenReturn(Mono.empty());

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders/1"));
		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
		verify(redis, never()).opsForValue();
		assertThat(exchange.getResponse().getStatusCode()).isNull();
	}
}
