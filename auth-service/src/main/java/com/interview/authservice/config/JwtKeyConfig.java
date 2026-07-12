package com.interview.authservice.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

@Configuration
public class JwtKeyConfig {

	@Bean
	public RSAKey rsaKey() throws Exception {
		return new RSAKeyGenerator(2048)
				.keyID(UUID.randomUUID().toString())
				.generate();
	}
}
