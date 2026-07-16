package com.interview.orderservice.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(ExecutorProperties.class)
public class AsyncConfig {

	@Bean("auditExecutor")
	public Executor auditExecutor(ExecutorProperties props) {
		ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
		pool.setCorePoolSize(props.audit().coreSize());
		pool.setMaxPoolSize(props.audit().maxSize());
		pool.setQueueCapacity(props.audit().queueCapacity());
		pool.setThreadNamePrefix("audit-");
		pool.initialize();
		return pool;
	}
}
