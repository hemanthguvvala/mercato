package com.interview.orderservice.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

@Configuration
public class SagaExecutorConfig {

	@Bean("reservationExecutor")
	public Executor reservationExecutor() {
		ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
		pool.setCorePoolSize(4);
		pool.setMaxPoolSize(8);
		pool.setQueueCapacity(50);
		pool.setThreadNamePrefix("saga-reserve-");
		pool.initialize();
		return new DelegatingSecurityContextExecutor(pool);
	}
}
