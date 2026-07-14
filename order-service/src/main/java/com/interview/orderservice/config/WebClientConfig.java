package com.interview.orderservice.config;

import java.time.Duration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServletBearerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
		// R20: bound connect + response time so a slow/hung payment-service can't pin order threads
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
				.responseTimeout(Duration.ofSeconds(5));
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.filter(new ServletBearerExchangeFilterFunction());
	}
}
