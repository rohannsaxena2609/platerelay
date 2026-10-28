package dev.platerelay.platform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PlateRelay API",
                version = "1.0.0",
                description = "Coordinate nearby surplus food rescue, safe reservations, and pickup handoffs.",
                contact = @Contact(name = "PlateRelay maintainers", url = "https://github.com/rohannsaxena2609/platerelay"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfiguration {}
