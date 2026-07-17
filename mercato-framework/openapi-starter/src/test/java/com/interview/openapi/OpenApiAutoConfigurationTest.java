package com.interview.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Proves the starter contributes a platform OpenAPI definition purely from classpath presence, and
 * that the JWT bearer scheme is wired so Swagger UI's "Authorize" sends `Authorization: Bearer ...`.
 */
class OpenApiAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration.class));

	@Test
	void providesOpenApiBeanWithJwtBearerScheme() {
		runner.run(context -> {
			assertThat(context).hasSingleBean(OpenAPI.class);
			OpenAPI api = context.getBean(OpenAPI.class);
			SecurityScheme scheme = api.getComponents().getSecuritySchemes().get("bearer-jwt");
			assertThat(scheme).isNotNull();
			assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
			assertThat(scheme.getScheme()).isEqualTo("bearer");
			assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
		});
	}

	@Test
	void backsOffWhenServiceDefinesItsOwnOpenApi() {
		runner.withBean("customOpenApi", OpenAPI.class, OpenAPI::new)
				.run(context -> assertThat(context).hasSingleBean(OpenAPI.class));
	}
}
