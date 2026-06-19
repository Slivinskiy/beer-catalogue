package com.haufe.beercatalogue.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.haufe.beercatalogue.domain.Beer;
import com.haufe.beercatalogue.domain.BeerType;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.exception.NotFoundException;
import com.haufe.beercatalogue.repository.BeerRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@ExtendWith(MockitoExtension.class)
class BeerServiceTest {
    @Mock
    private BeerRepository beerRepository;

    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private AccessService accessService;

    @InjectMocks
    private BeerService beerService;

    @Test
    void shouldReturnFilteredBeers() {
        final var manufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var beers = List.of(beer(1L, "Punk IPA", manufacturer));
        final var pageable = PageRequest.of(0, 20);
        when(beerRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Beer>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(beers));

        final var result = beerService.findAll("Punk", BeerType.IPA, new BigDecimal("5.60"), "Brew", pageable);

        assertEquals(beers, result.getContent());
    }

    @Test
    void shouldReturnBeerById() {
        final var manufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var beer = beer(1L, "Punk IPA", manufacturer);
        when(beerRepository.findById(1L)).thenReturn(Optional.of(beer));

        final var result = beerService.findById(1L);

        assertSame(beer, result);
    }

    @Test
    void shouldUploadBeerImage() {
        final var manufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var beer = beer(1L, "Punk IPA", manufacturer);
        final var file = new MockMultipartFile("file", "punk.png", "image/png", new byte[]{1, 2, 3});
        when(beerRepository.findById(1L)).thenReturn(Optional.of(beer));
        when(beerRepository.save(any(Beer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        beerService.uploadImage(1L, file);

        assertArrayEquals(new byte[]{1, 2, 3}, beer.getImage());
        assertEquals("image/png", beer.getImageContentType());
    }

    @Test
    void shouldReturnBeerImage() {
        final var manufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var beer = beer(1L, "Punk IPA", manufacturer);
        beer.setImage(new byte[]{1, 2, 3});
        beer.setImageContentType("image/png");
        when(beerRepository.findById(1L)).thenReturn(Optional.of(beer));

        final var result = beerService.getImage(1L);

        assertArrayEquals(new byte[]{1, 2, 3}, result.content());
        assertEquals("image/png", result.contentType());
    }

    @Test
    void shouldThrowWhenBeerIsMissing() {
        when(beerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> beerService.findById(1L));
    }

    @Test
    void shouldCreateBeer() {
        final var manufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var beer = beer(null, "Punk IPA", manufacturer);

        when(manufacturerRepository.findById(1L)).thenReturn(Optional.of(manufacturer));
        when(beerRepository.save(any(Beer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var result = beerService.create(beer);

        assertEquals("Punk IPA", result.getName());
        assertSame(manufacturer, result.getManufacturer());
    }

    @Test
    void shouldUpdateBeer() {
        final var oldManufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var newManufacturer = manufacturer(2L, "Guinness", "Ireland");

        final var existingBeer = beer(1L, "Old Beer", oldManufacturer);
        final var updatedBeer = beer(null, "New Beer", newManufacturer);
        updatedBeer.setAbv(new BigDecimal("6.50"));
        updatedBeer.setType(BeerType.STOUT);
        updatedBeer.setDescription("Updated description");

        when(beerRepository.findById(1L)).thenReturn(Optional.of(existingBeer));
        when(manufacturerRepository.findById(2L)).thenReturn(Optional.of(newManufacturer));
        when(beerRepository.save(any(Beer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var result = beerService.update(1L, updatedBeer);

        assertEquals("New Beer", result.getName());
        assertEquals(new BigDecimal("6.50"), result.getAbv());
        assertEquals(BeerType.STOUT, result.getType());
        assertEquals("Updated description", result.getDescription());
        assertSame(newManufacturer, result.getManufacturer());
    }

    @Test
    void shouldDeleteBeer() {
        final var manufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var beer = beer(1L, "Punk IPA", manufacturer);
        when(beerRepository.findById(1L)).thenReturn(Optional.of(beer));

        beerService.delete(1L);

        verify(beerRepository).delete(beer);
    }

    @Test
    void shouldThrowWhenBeerManufacturerIdIsMissing() {
        final var beer = new Beer("Punk IPA", new BigDecimal("5.60"), BeerType.IPA, "Classic IPA", null);

        assertThrows(IllegalArgumentException.class, () -> beerService.create(beer));
    }

    @Test
    void shouldThrowWhenBeerManufacturerDoesNotExist() {
        final var manufacturer = new Manufacturer("Missing", "Unknown");
        manufacturer.setId(99L);
        final var beer = beer(null, "Punk IPA", manufacturer);
        when(manufacturerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> beerService.create(beer));
    }

    @Test
    void shouldThrowWhenBeerImageIsMissing() {
        final var manufacturer = manufacturer(1L, "BrewDog", "Scotland");
        final var beer = beer(1L, "Punk IPA", manufacturer);
        when(beerRepository.findById(1L)).thenReturn(Optional.of(beer));

        assertThrows(NotFoundException.class, () -> beerService.getImage(1L));
    }

    private Manufacturer manufacturer(final Long id, final String name, final String countryOfOrigin) {
        final var manufacturer = new Manufacturer(name, countryOfOrigin);
        manufacturer.setId(id);
        return manufacturer;
    }

    private Beer beer(final Long id, final String name, final Manufacturer manufacturer) {
        final var beer = new Beer(name, new BigDecimal("5.60"), BeerType.IPA, "Classic IPA", manufacturer);
        beer.setId(id);
        return beer;
    }
}
