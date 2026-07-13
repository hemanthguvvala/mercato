package com.interview.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public KeyResolver userKeyResolver() {
		return exchange -> ReactiveSecurityContextHolder.getContext().map(ctx -> ctx.getAuthentication().getName())
				.defaultIfEmpty("anonymouds");
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
