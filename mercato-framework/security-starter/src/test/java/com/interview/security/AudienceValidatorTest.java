package com.interview.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * R36 (part 2, enforce side): the resource server must accept a token stamped for our platform
 * audience and reject one that is stamped for something else — or carries no {@code aud} at all.
 */
class AudienceValidatorTest {

	private final OAuth2TokenValidator<Jwt> validator = new AudienceValidator("mercato");

	@Test
	void acceptsTokenWithExpectedAudience() {
		assertThat(validator.validate(jwt(List.of("mercato"))).hasErrors()).isFalse();
	}

	@Test
	void rejectsTokenWithWrongAudience() {
		assertThat(validator.validate(jwt(List.of("some-other-app"))).hasErrors()).isTrue();
	}

	@Test
	void rejectsTokenWithNoAudience() {
		assertThat(validator.validate(jwtNoAudience()).hasErrors()).isTrue();
	}

	private Jwt jwt(List<String> audience) {
		return Jwt.withTokenValue("token").header("alg", "RS256").audience(audience).subject("alice").build();
	}

	private Jwt jwtNoAudience() {
		return Jwt.withTokenValue("token").header("alg", "RS256").subject("alice").build();
	}
}
