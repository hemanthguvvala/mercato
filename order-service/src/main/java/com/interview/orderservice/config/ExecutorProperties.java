package com.interview.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.executor")
public record ExecutorProperties(Pool saga, Pool audit) {

	public record Pool(int coreSize, int maxSize, int queueCapacity) {
	}

}
