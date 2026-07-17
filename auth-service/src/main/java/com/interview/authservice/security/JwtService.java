package com.interview.authservice.security;

import java.security.PrivateKey;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.interview.authservice.config.JwtProperties;
import com.nimbusds.jose.jwk.RSAKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues signed JWTs. This service only SIGNS tokens (issuance); validation
 * lives in the gateway and the resource services, which hold the same secret
 * and verify HS256 locally.
 */
@Service
public class JwtService {

	private final PrivateKey privateKey;
	private final String keyId;
	private final String issuer;
	private final long expiryMs;
	private final String audience;

	public JwtService(RSAKey rsaKey, JwtProperties jwtProperties) throws Exception {
		this.privateKey = rsaKey.toPrivateKey();
		this.keyId = rsaKey.getKeyID();
		this.issuer = jwtProperties.issuer();
		this.expiryMs = jwtProperties.expiryMs();
		this.audience = jwtProperties.audience();
	}

	public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
		Date now = new Date();
		List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
		return Jwts.builder().header().keyId(keyId).and().subject(username).issuer(issuer).audience().add(audience)
				.and().claim("roles", roles).issuedAt(now).expiration(new Date(now.getTime() + expiryMs))
				.signWith(privateKey, Jwts.SIG.RS256).compact();
	}
}
