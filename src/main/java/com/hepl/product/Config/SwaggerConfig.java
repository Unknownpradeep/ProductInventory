package com.hepl.product.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {

        SecurityScheme securityScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme);

        return new OpenAPI()
                .info(new Info()
                        .title("Product Inventory System API")
                        .version("1.0.0")
                        .description("PIS REST API with JWT Authentication"))
                .servers(List.of(
                        new Server()
                                .url("https://5thq69dw-8080.inc1.devtunnels.ms/")
                                .description("Dev Tunnel Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(components);
    }
}
