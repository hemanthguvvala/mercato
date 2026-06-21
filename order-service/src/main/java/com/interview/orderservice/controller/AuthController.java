package com.interview.orderservice.controller;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.orderservice.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	public AuthController(JwtService jwtService, AuthenticationManager authenticationManager) {
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
	}

	@PostMapping("/login")
	public Map<String, String> login(@RequestBody LoginRequest request) {

		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username, request.password));
		return Map.of("token", jwtService.generateToken(authentication.getName()));
	}

	public record LoginRequest(String username, String password) {
	}
}
