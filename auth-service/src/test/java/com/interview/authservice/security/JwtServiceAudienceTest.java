package com.interview.authservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.interview.authservice.config.JwtProperties;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * R36 (part 2, mint side): a freshly issued token must carry the configured {@code aud} claim, so the
 * resource servers can later enforce it. We sign with a throwaway RSA key and re-parse the token with
 * the matching public key — no Spring context, no network.
 */
class JwtServiceAudienceTest {

	@Test
	void mintedTokenCarriesConfiguredAudienceAndIssuer() throws Exception {
		RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
		JwtService jwtService = new JwtService(rsaKey,
				new JwtProperties(900_000, "http://localhost:8084", "mercato"));

		String token = jwtService.generateToken("alice", List.of(new SimpleGrantedAuthority("ROLE_USER")));

		Claims claims = Jwts.parser()
				.verifyWith(rsaKey.toPublicKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();

		assertThat(claims.getAudience()).contains("mercato");
		assertThat(claims.getIssuer()).isEqualTo("http://localhost:8084");
	}
}
