package com.interview.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Proves the starter's auto-configuration wires itself correctly from nothing but a classpath
 * presence — the exact behaviour a consuming service relies on when it "just adds the dependency".
 *
 * We use Spring Boot's {@code *ContextRunner}s (the idiomatic way to test auto-configuration): each
 * runner boots a throwaway context of a given web type, with only THIS auto-config registered, and
 * lets us assert which beans materialised. No Eureka, no full application, no network.
 */
class JwtValidationAutoConfigurationTest {

	private static final String[] PROPS = {
			"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8084/oauth2/jwks",
			"security.jwt.issuer=http://localhost:8084" };

	@Test
	void servletApp_getsServletDecoderOnly() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(JwtValidationAutoConfiguration.class))
				.withPropertyValues(PROPS)
				.run(context -> {
					assertThat(context).hasSingleBean(JwtDecoder.class);
					assertThat(context).doesNotHaveBean(ReactiveJwtDecoder.class);
				});
	}

	@Test
	void reactiveApp_getsReactiveDecoderOnly() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(JwtValidationAutoConfiguration.class))
				.withPropertyValues(PROPS)
				.run(context -> {
					assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
					assertThat(context).doesNotHaveBean(JwtDecoder.class);
				});
	}

	@Test
	void nonWebApp_getsNoDecoder() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(JwtValidationAutoConfiguration.class))
				.withPropertyValues(PROPS)
				.run(context -> {
					assertThat(context).doesNotHaveBean(JwtDecoder.class);
					assertThat(context).doesNotHaveBean(ReactiveJwtDecoder.class);
				});
	}

	@Test
	void userDefinedDecoder_makesStarterBackOff() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(JwtValidationAutoConfiguration.class))
				.withPropertyValues(PROPS)
				.withBean("customDecoder", JwtDecoder.class, () -> token -> null)
				.run(context -> {
					// @ConditionalOnMissingBean: the starter must NOT add a second decoder
					assertThat(context).hasSingleBean(JwtDecoder.class);
					assertThat(context).hasBean("customDecoder");
				});
	}
}
