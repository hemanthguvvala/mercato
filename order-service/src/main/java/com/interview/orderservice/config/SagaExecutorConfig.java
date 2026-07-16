package com.interview.orderservice.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

@EnableConfigurationProperties(ExecutorProperties.class)
@Configuration
public class SagaExecutorConfig {

	@Bean("reservationExecutor")
	public Executor reservationExecutor(ExecutorProperties props) {
		ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
		pool.setCorePoolSize(props.saga().coreSize());
		pool.setMaxPoolSize(props.saga().maxSize());
		pool.setQueueCapacity(props.saga().queueCapacity());
		pool.setThreadNamePrefix("saga-reserve-");
		pool.initialize();
		return new DelegatingSecurityContextExecutor(pool);
	}
}
