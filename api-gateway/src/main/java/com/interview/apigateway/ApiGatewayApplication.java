package com.interview.apigateway;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.Customizer;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public KeyResolver userKeyResolver() {
		return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-User"))
				.defaultIfEmpty("anonymous");
	}
	
	@Bean
	public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret-key}") String secret) {
		SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}
	
	@Bean
	public SecurityWebFilterChain filterChain( ServerHttpSecurity httpSecurity) {
		return httpSecurity
				.csrf(csrf -> csrf.disable())
				.authorizeExchange(auth -> 
				auth.pathMatchers("/auth/**").permitAll()
				.pathMatchers("/actuator/**").permitAll()
				.pathMatchers(HttpMethod.GET,"/products/**").permitAll()
				.anyExchange().authenticated())
				.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())).build();
	}
}
