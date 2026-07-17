package com.interview.apigateway.ratelimit;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

	private final ReactiveStringRedisTemplate redis;
	private final int limit;
	private final Duration window;

	public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate,
			@Value("${gateway.rate-limit.requests:10}") int limit,
			@Value("${gateway.rate-limit.window-seconds:1}") long windowSeconds) {
		this.limit = limit;
		this.window = Duration.ofSeconds(windowSeconds);
		this.redis = redisTemplate;
	}

	@Override
	public int getOrder() {
		return -1;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		if (!path.startsWith("/products")) {
			return chain.filter(exchange);
		}

		return resolveKey(exchange).flatMap(key -> {
			String redisKey = "rate_limit:products:" + key;
			return redis.opsForValue().increment(redisKey).flatMap(count -> {
				Mono<Boolean> ensureKey = (count == 1L) ? redis.expire(redisKey, window) : Mono.just(true);
				return ensureKey.then(Mono.defer(() -> {
					if (count > limit) {
						exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
						return exchange.getResponse().setComplete();
					}
					return chain.filter(exchange);
				}));
			});
		}).onErrorResume(e -> {
			exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
			return exchange.getResponse().setComplete();
		});
	}

	private Mono<String> resolveKey(ServerWebExchange exchange) {
		return ReactiveSecurityContextHolder.getContext().map(ctx -> ctx.getAuthentication().getName())
				.defaultIfEmpty("anonymous");
	}

}
