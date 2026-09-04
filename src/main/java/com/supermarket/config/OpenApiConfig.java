package com.supermarket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    public static final String ADMIN_BASIC_SCHEME = "adminBasic";

    @Bean
    public OpenAPI supermarketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Supermarket API")
                        .version("1.0.0")
                        .description("Checkout and customer order endpoints are public. "
                                + "Administrator endpoints use HTTP Basic authentication. "
                                + "Money values are represented as two-decimal strings, for example 55.00."))
                .components(new Components()
                        .addSecuritySchemes(ADMIN_BASIC_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Configured local administrator credentials"))
                        .addSchemas("Money", new StringSchema()
                                .description("Two-decimal monetary amount")
                                .example("55.00")));
    }
}
