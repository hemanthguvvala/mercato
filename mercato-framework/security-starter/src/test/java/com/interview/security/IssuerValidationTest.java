package com.interview.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/**
 * R36 (part 1): the resource server must reject a token whose {@code iss} claim is not our auth
 * server, even when the signature and expiry are otherwise valid. This exercises the exact validator
 * the starter's decoder installs — {@link JwtValidators#createDefaultWithIssuer} — proving that
 * (a) issuer mismatches are rejected and (b) the default timestamp checks are still applied (i.e. we
 * did not lose expiry validation by overriding the decoder).
 *
 * We test the validator directly rather than booting the NimbusJwtDecoder, so no JWKS endpoint or
 * network is involved — signature verification is a separate concern already covered by Spring.
 */
class IssuerValidationTest {

	private static final String TRUSTED_ISSUER = "http://localhost:8084";

	private final OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(TRUSTED_ISSUER);

	@Test
	void acceptsTokenFromTrustedIssuer() {
		Jwt token = jwt(TRUSTED_ISSUER, Instant.now().plusSeconds(300));

		assertThat(validator.validate(token).hasErrors()).isFalse();
	}

	@Test
	void rejectsTokenFromWrongIssuer() {
		Jwt token = jwt("http://evil-issuer", Instant.now().plusSeconds(300));

		assertThat(validator.validate(token).hasErrors()).isTrue();
	}

	@Test
	void rejectsExpiredTokenEvenFromTrustedIssuer() {
		Jwt token = jwt(TRUSTED_ISSUER, Instant.now().minusSeconds(300));

		assertThat(validator.validate(token).hasErrors()).isTrue();
	}

	private Jwt jwt(String issuer, Instant expiresAt) {
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.issuer(issuer)
				.subject("alice")
				.issuedAt(expiresAt.minusSeconds(60)) // always before expiry (builder enforces iat < exp)
				.expiresAt(expiresAt)
				.build();
	}
}
