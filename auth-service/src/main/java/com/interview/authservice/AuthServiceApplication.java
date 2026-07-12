package com.interview.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.interview.authservice.config.JwtProperties;

/**
 * Auth service — owns login + JWT issuance only.
 *
 * Extracted from order-service (R42) so that authentication is no longer coupled to a
 * business service: an order-service crash can no longer take down login for the whole system.
 * Tokens are stateless HS256 JWTs, so the gateway and other services validate them locally —
 * this service is only on the critical path at login/refresh, not per request.
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
