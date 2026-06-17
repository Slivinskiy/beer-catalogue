package com.haufe.beercatalogue.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ManufacturerRequest(
        @NotBlank String name,
        @NotBlank String countryOfOrigin
) {
}
