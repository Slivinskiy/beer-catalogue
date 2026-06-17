package com.haufe.beercatalogue.controller.dto;

import com.haufe.beercatalogue.domain.Manufacturer;

public record ManufacturerResponse(
        Long id,
        String name,
        String countryOfOrigin
) {

    public static ManufacturerResponse from(final Manufacturer manufacturer) {
        return new ManufacturerResponse(
                manufacturer.getId(),
                manufacturer.getName(),
                manufacturer.getCountryOfOrigin()
        );
    }
}
