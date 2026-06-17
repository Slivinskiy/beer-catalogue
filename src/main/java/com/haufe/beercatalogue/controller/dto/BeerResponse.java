package com.haufe.beercatalogue.controller.dto;

import java.math.BigDecimal;

import com.haufe.beercatalogue.domain.Beer;
import com.haufe.beercatalogue.domain.BeerType;

public record BeerResponse(
        Long id,
        String name,
        BigDecimal abv,
        BeerType type,
        String description,
        Long manufacturerId
) {

    public static BeerResponse from(final Beer beer) {
        return new BeerResponse(
                beer.getId(),
                beer.getName(),
                beer.getAbv(),
                beer.getType(),
                beer.getDescription(),
                beer.getManufacturer().getId()
        );
    }
}
