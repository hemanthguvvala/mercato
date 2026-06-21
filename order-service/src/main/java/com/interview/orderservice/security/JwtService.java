package com.interview.orderservice.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.interview.orderservice.config.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;

	private final long expiryMs;
	
	public JwtService(JwtProperties jwtProperties) {
		this.key = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes());
		this.expiryMs  = jwtProperties.expiryMs();
		
	}

	public String generateToken(String username) {
		Date now = new Date();
		return Jwts.builder()
				.subject(username)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expiryMs))
				.signWith(key)
				.compact();

	}

	public String extractUsername(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public boolean isValid(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
