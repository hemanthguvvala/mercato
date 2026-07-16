package com.interview.orderservice.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean("auditExecutor")
	public Executor auditExecutor() {
		ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
		pool.setCorePoolSize(2);
		pool.setMaxPoolSize(4);
		pool.setQueueCapacity(100);
		pool.setThreadNamePrefix("audit-");
		pool.initialize();
		return pool;
	}
}
