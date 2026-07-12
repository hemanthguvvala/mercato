package com.interview.authservice.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@RestController
public class JwksController {

	private final RSAKey publicJwk;
	
	public JwksController(RSAKey rsaKey) {
		this.publicJwk = rsaKey.toPublicJWK();
	}
	
	@GetMapping("/oauth2/jwks")
	public Map<String, Object> keys(){
		return new JWKSet(publicJwk).toJSONObject();
	}
}
