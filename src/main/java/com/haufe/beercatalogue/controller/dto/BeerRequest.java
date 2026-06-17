package com.haufe.beercatalogue.controller.dto;

import java.math.BigDecimal;

import com.haufe.beercatalogue.domain.BeerType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BeerRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal abv,
        @NotNull BeerType type,
        @NotBlank @Size(max = 1000) String description,
        @NotNull Long manufacturerId
) {
}
