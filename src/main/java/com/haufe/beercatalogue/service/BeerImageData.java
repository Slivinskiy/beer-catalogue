package com.haufe.beercatalogue.service;

public record BeerImageData(
        byte[] content,
        String contentType
) {
}
