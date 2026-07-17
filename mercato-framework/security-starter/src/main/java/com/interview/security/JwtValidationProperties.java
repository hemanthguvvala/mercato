package com.interview.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtValidationProperties(String issuer, String audience) {

}
