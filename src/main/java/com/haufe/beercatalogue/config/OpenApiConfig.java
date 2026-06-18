package com.haufe.beercatalogue.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Beer Catalogue API",
                version = "v1",
                description = "REST API for managing beers and manufacturers."
        )
)
public class OpenApiConfig {
}
