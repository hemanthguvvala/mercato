package com.interview.catalogservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/products/**").permitAll()
				.anyRequest().authenticated())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
				.csrf(csrf -> csrf.disable())
				.headers(h -> h.frameOptions(f -> f.sameOrigin()))
				.build();

	}
}
