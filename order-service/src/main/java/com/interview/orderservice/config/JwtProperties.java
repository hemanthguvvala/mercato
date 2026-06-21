package com.interview.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix ="jwt")
public record JwtProperties(String secretKey, long expiryMs) {

}
